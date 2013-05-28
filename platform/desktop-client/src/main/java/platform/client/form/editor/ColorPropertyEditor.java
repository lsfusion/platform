package platform.client.form.editor;

import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import platform.client.ClientResourceBundle;
import platform.client.logics.classes.ClientColorClass;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.reflect.Field;

public class ColorPropertyEditor extends DialogBasedPropertyEditor {
    private JColorChooser colorChooser;
    private JDialog chooserDialog;

    private boolean isColorChosen;
    private Color chosenColor;

    public ColorPropertyEditor(Object value) {
        super();

        final Color initialColor = value != null ? (Color) value : ClientColorClass.getDefaultValue();

        setBackground(initialColor);

        colorChooser = new JColorChooser(initialColor);

        colorChooser.getSelectionModel().addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                setBackground(colorChooser.getColor());
            }
        });

        ActionListener okListener = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                setChoosenColor(colorChooser.getColor());
            }
        };

        ActionListener cancelListener = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                isColorChosen = false;
                setBackground(initialColor);
            }
        };

        ActionListener nullifyListener = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                chooserDialog.setVisible(false);
                setChoosenColor(null);
            }
        };

        chooserDialog = JColorChooser.createDialog(null, ClientResourceBundle.getString("form.choose.color"), true, colorChooser, okListener, cancelListener);

        //довольно дико конечно, но пока не хочется замарачиваться со своим диалогом...
        Preconditions.checkState(chooserDialog.getClass().getName().equals("javax.swing.ColorChooserDialog"));
        try {
            JButton nullifyButton = new JButton("Nullify");
            nullifyButton.addActionListener(nullifyListener);

            Field cancelBtnField = chooserDialog.getClass().getDeclaredField("cancelButton");
            cancelBtnField.setAccessible(true);

            JButton cancelButton = (JButton) cancelBtnField.get(chooserDialog);
            Container buttonPane = cancelButton.getParent();
            buttonPane.add(nullifyButton, null, 2);
        } catch (Exception e) {
            Throwables.propagate(e);
        }
    }

    private void setChoosenColor(Color chosenColor) {
        this.isColorChosen = true;
        this.chosenColor = chosenColor;
        if (chosenColor != null) {
            this.setBackground(ClientColorClass.getDefaultValue());
        }
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
