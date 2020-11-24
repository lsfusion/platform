package lsfusion.client.form.property.cell.classes.controller;

import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import lsfusion.client.ClientResourceBundle;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.lang.reflect.Field;

public class ColorPropertyEditor extends DialogBasedPropertyEditor {
    private JColorChooser colorChooser;
    private JDialog chooserDialog;

    private boolean isColorChosen;
    private Color chosenColor;

    public ColorPropertyEditor(Object value) {
        super();

        final Color initialColor = (value instanceof Color) ? (Color) value : null;

        setBackground(initialColor);

        colorChooser = initialColor != null ? new JColorChooser(initialColor) : new JColorChooser();

        colorChooser.getSelectionModel().addChangeListener(e -> setBackground(colorChooser.getColor()));

        ActionListener okListener = e -> setChosenColor(colorChooser.getColor());

        ActionListener cancelListener = e -> {
            isColorChosen = false;
            setBackground(initialColor);
        };

        ActionListener nullifyListener = e -> {
            chooserDialog.setVisible(false);
            setChosenColor(null);
        };

        chooserDialog = JColorChooser.createDialog(null, ClientResourceBundle.getString("form.choose.color"), true, colorChooser, okListener, cancelListener);

        //довольно дико конечно, но пока не хочется замарачиваться со своим диалогом...
        Preconditions.checkState(chooserDialog.getClass().getName().equals("javax.swing.ColorChooserDialog"));
        try {
            Field cancelBtnField = chooserDialog.getClass().getDeclaredField("cancelButton");
            cancelBtnField.setAccessible(true);

            JButton cancelButton = (JButton) cancelBtnField.get(chooserDialog);
            Container buttonPane = cancelButton.getParent();
            
            JButton resetButton = ((JButton) buttonPane.getComponent(2));
            resetButton.addActionListener(nullifyListener);
        } catch (Exception e) {
            Throwables.propagate(e);
        }
    }

    private void setChosenColor(Color chosenColor) {
        this.isColorChosen = true;
        this.chosenColor = chosenColor;
        this.setBackground(chosenColor);
    }

    private void setBackground(Color color) {
        editorStub.setBackground(color);
    }

    @Override
    public void showDialog(Point desiredLocation) {
        chooserDialog.setVisible(true);
    }

    @Override
    public boolean valueChanged() {
        return isColorChosen;
    }

    @Override
    public Object getCellEditorValue() {
        return chosenColor;
    }
}
