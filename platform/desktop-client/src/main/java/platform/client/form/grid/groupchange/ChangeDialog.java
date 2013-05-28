package platform.client.form.grid.groupchange;

import org.apache.log4j.Logger;
import platform.client.ClientResourceBundle;
import platform.client.form.ClientFormController;
import platform.client.form.ItemAdapter;
import platform.client.form.cell.SingleCellTable;
import platform.client.form.grid.GridTable;
import platform.client.form.grid.GridTableModel;
import platform.client.logics.ClientGroupObjectValue;
import platform.client.logics.ClientPropertyDraw;
import platform.interop.KeyStrokes;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Vector;

import static javax.swing.JOptionPane.CANCEL_OPTION;
import static javax.swing.JOptionPane.OK_OPTION;

public class ChangeDialog extends JDialog {
    private static Logger logger = Logger.getLogger(ChangeDialog.class);

    private int result = CANCEL_OPTION;

    private JPanel selectionPanel;

    private JComboBox cbValueType;
    private JComboBox cbMainProperties;

    private GroupValueView valueView;
    private GroupValueView valueViews[] = new GroupValueView[]{
            new ExactValueView(null),
            new PropertyValueView(null)
    };
    private final GridTable gridTable;
    private GridTableModel model;
    private ClientFormController form;

    public ChangeDialog(JFrame owner, GridTable gridTable) {
        super(owner, ClientResourceBundle.getString("form.grid.group.groupchange"), true);

        this.gridTable = gridTable;
        this.model = gridTable.getModel();
        this.form = gridTable.getForm();

        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(owner);

        initializeComponents();

        cbValueType.addItemListener(new ItemAdapter() {
            @Override
            public void itemSelected(ItemEvent e) {
                changeTypeUpdated();
            }
        });

        cbMainProperties.addItemListener(new ItemAdapter() {
            @Override
            public void itemSelected(ItemEvent e) {
                mainPropertyChanged();
            }
        });
        changeTypeUpdated();

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowActivated(WindowEvent e) {
                valueView.requestFocusInWindow();
                //чтобы срабатывал только один раз...
                removeWindowListener(this);
            }
        });
    }

    private void changeTypeUpdated() {
        if (valueView != null) {
            selectionPanel.remove(valueView);
        }

        valueView = valueViews[cbValueType.getSelectedIndex()];

        selectionPanel.add(valueView);

        mainPropertyChanged();
    }

    private void mainPropertyChanged() {
        ColumnProperty mainProperty = getMainProperty();
        if (mainProperty != null) {
            valueView.mainPropertyChanged(mainProperty);
        }
        selectionPanel.revalidate();
        pack();
    }

    private void initializeComponents() {
        ColumnProperty initialProperty = null;
        int selectedColumn = gridTable.getSelectedColumn();

        int n = model.getColumnCount();
        Vector<ColumnProperty> mainProperties = new Vector<ColumnProperty>();
        for (int i = 0; i < n; ++i) {
            ColumnProperty columnProperty = new ColumnProperty(model, i);
            if (model.isCellEditable(0, i)) {
                if (i >= selectedColumn && initialProperty == null) {
                    initialProperty = columnProperty;
                }
                mainProperties.add(columnProperty);
            }
        }

        cbMainProperties = new JComboBox(mainProperties);
        cbMainProperties.setSelectedItem(initialProperty);

        cbValueType = new JComboBox(new String[]{ClientResourceBundle.getString("form.grid.group.groupchange.value"), ClientResourceBundle.getString("form.grid.group.groupchange.property")});

        AbstractAction okAction = new ResultAction(ClientResourceBundle.getString("form.grid.group.groupchange.take"), true);
        AbstractAction cancelAction = new ResultAction(ClientResourceBundle.getString("form.grid.group.groupchange.cancel"), false);

        JButton okBut = new JButton(okAction);
        okBut.getActionMap().put("applyGroupChange", okAction);
        okBut.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStrokes.getAltEnter(), "applyGroupChange");

        JButton cancelBut = new JButton(cancelAction);
        cancelBut.getActionMap().put("cancelGroupChange", cancelAction);
        cancelBut.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStrokes.getEscape(), "cancelGroupChange");

        selectionPanel = new JPanel();
        selectionPanel.setLayout(new BoxLayout(selectionPanel, BoxLayout.X_AXIS));
        selectionPanel.add(cbMainProperties);
        selectionPanel.add(cbValueType);

        JPanel buttonPanel = new JPanel(new FlowLayout());
        buttonPanel.add(okBut);
        buttonPanel.add(cancelBut);

        Container pane = getContentPane();
        pane.setLayout(new BorderLayout());
        pane.add(selectionPanel, BorderLayout.NORTH);
        pane.add(buttonPanel, BorderLayout.SOUTH);
    }

    public ColumnProperty getMainProperty() {
        return (ColumnProperty) cbMainProperties.getSelectedItem();
    }

    public ColumnProperty getGetterProperty() {
        return valueView.getGetterProperty();
    }

    public Object getSelectedValue() {
        return valueView.getSelectedValue();
    }

    private void closeDialog(boolean apply) {
        result = apply ? OK_OPTION : CANCEL_OPTION;
        if (valueView instanceof ExactValueView) {
            if (form.commitCurrentEditing()) {
                return;
            }
        }
        setVisible(false);
    }

    public boolean prompt() {
        setVisible(true);
        return result == OK_OPTION;
    }

    private class ResultAction extends AbstractAction {
        private final boolean apply;

        private ResultAction(String name, boolean apply) {
            super(name);
            this.apply = apply;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            closeDialog(apply);
        }
    }

    public static abstract class GroupValueView extends JPanel {
        public GroupValueView() {
            setLayout(new BorderLayout());
        }

        public ColumnProperty getGetterProperty() {
            return null;
        }

        public Object getSelectedValue() {
            return null;
        }

        public abstract void mainPropertyChanged(ColumnProperty property);
    }

    private class ExactValueView extends GroupValueView {
        public final ExactValueTable valueTable;
        private Object selectedValue;

        public ExactValueView(ColumnProperty property) {
            super();

            setBorder(new JComboBox().getBorder());

            valueTable = new ExactValueTable();

            if (property != null) {
                mainPropertyChanged(property);
            }

            add(valueTable, BorderLayout.CENTER);
        }

        public boolean requestFocusInWindow() {
            return valueTable.requestFocusInWindow();
        }

        public void mainPropertyChanged(ColumnProperty columnProperty) {
            ClientPropertyDraw property = columnProperty.property;

            valueTable.setProperty(property);
            valueTable.setValue(gridTable.getSelectedValue(property, columnProperty.columnKey));

            //todo: this is broken
//            try {
//                selectedValue = form.getPropertyChangeValue(property);
//            } catch (RemoteException e) {
//                logger.error(ClientResourceBundle.getString("form.grid.group.groupchange.failed.to.retrieve.property.value"));
//                selectedValue = null;
//            }

            revalidate();
        }

        public Object getSelectedValue() {
            return selectedValue;
        }

        private class ExactValueTable extends SingleCellTable {
            public ExactValueTable() {
                super(new ClientGroupObjectValue());
            }

            public boolean isPressed(int row, int column) {
                return false;
            }

            public Color getBackgroundColor(int row, int column) {
                return null;
            }

            public Color getForegroundColor(int row, int column) {
                return null;
            }

            public ClientFormController getForm() {
                return form;
            }
        }
    }

    private class PropertyValueView extends GroupValueView {
        private JComboBox cbGetterProperties = new JComboBox();

        public PropertyValueView(ColumnProperty property) {
            add(cbGetterProperties);
            if (property != null) {
               mainPropertyChanged(property);
            }
        }

        @Override
        public boolean requestFocusInWindow() {
            return cbGetterProperties.requestFocusInWindow();
        }

        @Override
        public void mainPropertyChanged(ColumnProperty iproperty) {
            ClientPropertyDraw property = iproperty.property;
            ClientGroupObjectValue columnKey = iproperty.columnKey;

            //todo: this is broken
//            boolean compatible[];
//            try {
//                compatible = form.getCompatibleProperties(property, model.getColumnProperties());
//            } catch (RemoteException e) {
//                logger.error(ClientResourceBundle.getString("form.grid.group.groupchange.error.reading.is.compatible"), e);
//                compatible = new boolean[model.getColumnCount()];
//            }
//
//            List<ColumnProperty> options = new ArrayList<ColumnProperty>();
//            for (int i = 0; i < model.getColumnCount(); ++i) {
//                if (compatible[i] && !(model.getColumnProperty(i).getID() == property.getID() && columnKey.equals(model.getColumnKey(i)))) {
//                    options.add(new ColumnProperty(model, i));
//                }
//            }
//
//            cbGetterProperties.setModel(new DefaultComboBoxModel(options.toArray()));
//            revalidate();
        }

        @Override
        public ColumnProperty getGetterProperty() {
            return (ColumnProperty) cbGetterProperties.getSelectedItem();
        }
    }
}
