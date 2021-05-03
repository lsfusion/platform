package lsfusion.client.form.property.cell.classes.controller;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.GlazedLists;
import ca.odell.glazedlists.swing.AutoCompleteSupport;
import lsfusion.client.base.view.ClientColorUtils;
import lsfusion.client.form.property.ClientPropertyDraw;
import lsfusion.client.form.property.cell.controller.PropertyTableCellEditor;

import javax.swing.*;
import javax.swing.plaf.basic.BasicComboBoxUI;
import javax.swing.plaf.basic.BasicComboPopup;
import javax.swing.plaf.basic.ComboPopup;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.text.ParseException;
import java.util.EventObject;

import static lsfusion.base.ResourceUtils.readImage;
import static lsfusion.client.base.view.SwingDefaults.getTableCellMargins;

public abstract class TextFieldPropertyEditor extends JFormattedTextField implements PropertyEditor {
    private static final String CANCEL_EDIT_ACTION = "reset-field-edit";

    private static ImageIcon refreshIcon = readImage("refresh.png");

    protected PropertyTableCellEditor tableEditor;

    boolean hasList = false;
    JComboBox suggestBox = null;

    TextFieldPropertyEditor(ClientPropertyDraw property) {
        super();
        if (hasList) {

            Object[] elements = new Object[]{"Cat", "CDog", "Lion", "Mouse"};

            suggestBox = new JComboBox();

            suggestBox.setUI(new BasicComboBoxUI() {

                @Override
                protected ComboPopup createPopup() {
                    return new BasicComboPopup(comboBox) {
                        @Override
                        protected void configurePopup() {
                            super.configurePopup();
                            JButton refreshButton = new JButton(refreshIcon);
                            refreshButton.addActionListener(e -> System.out.println("refresh!"));
                            add(refreshButton);
                            setOpaque(true);
                            setBackground(Color.WHITE);
                        }
                    };
                }

                @Override
                protected JButton createArrowButton() {
                    return new JButton() {
                        public int getWidth() {
                            return 0;
                        }
                    };
                }


            });

            EventList<Object> items = GlazedLists.eventListOf(elements);

            //AutoCompleteDecorator.decorate(this);
            AutoCompleteSupport.install(suggestBox, items);

            suggestBox.addItemListener(new ItemListener() {
                @Override
                public void itemStateChanged(ItemEvent e) {
                    //here we can update items
                    //items.add("Another cat");
                }
            });

        } else {

            Insets insets = getTableCellMargins();
            setBorder(BorderFactory.createEmptyBorder(insets.top, insets.left, insets.bottom, insets.right - 1));
            setOpaque(true);

            if (property != null) {
                if (property.design != null) 
                    ClientColorUtils.designComponent(this, property.design);

                Integer valueAlignment = property.getSwingValueAlignment();
                if (valueAlignment != null) {
                    setHorizontalAlignment(valueAlignment);
                }
            }
            
        }

        addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                tableEditor.stopCellEditing();
                if(hasList) {
                    suggestBox.firePopupMenuCanceled();
                }
            }
        });

        getActionMap().put(CANCEL_EDIT_ACTION, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                tableEditor.cancelCellEditing();
                if(hasList) {
                    suggestBox.firePopupMenuCanceled();
                }
            }
        });
    }

    public void setTableEditor(PropertyTableCellEditor tableEditor) {
        this.tableEditor = tableEditor;
    }

    public Component getComponent(Point tableLocation, Rectangle cellRectangle, EventObject editEvent) {
        return hasList ? suggestBox : this;
    }

    public boolean stopCellEditing() {
        try {
            commitEdit();
        } catch (ParseException e) {
            return false;
        }

        return true;
    }

    public void cancelCellEditing() { }

    public Object getCellEditorValue() {
        return this.getValue();
    }

    @Override
    public String toString() {
        if (tableEditor != null) {
            return "TextFieldEditor[" + tableEditor.getTable().getName() + "]: " + super.toString();
        } else {
            return super.toString();
        }
    }

    @Override
    public void replaceSelection(String content){
        if(content.endsWith("\n"))
            content = content.substring(0, content.length()-1);
        try {
            super.replaceSelection(content);
        } catch (IllegalArgumentException e) { // strange java bug, so just suppress that exception
        }
    }
}
