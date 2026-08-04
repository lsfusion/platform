package lsfusion.client.form.property.cell.classes.controller;

import lsfusion.client.ClientResourceBundle;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

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

        JDialog dialog = new JDialog((Frame) null, ClientResourceBundle.getString("form.choose.color"), true);
        dialog.setLayout(new BorderLayout());
        dialog.add(colorChooser, BorderLayout.CENTER);

        JButton okButton = new JButton(UIManager.getString("ColorChooser.okText"));
        okButton.addActionListener(e -> {
            okListener.actionPerformed(e);
            dialog.setVisible(false);
        });

        JButton cancelButton = new JButton(UIManager.getString("ColorChooser.cancelText"));
        cancelButton.addActionListener(e -> {
            cancelListener.actionPerformed(e);
            dialog.setVisible(false);
        });

        JButton resetButton = new JButton(UIManager.getString("ColorChooser.resetText"));
        resetButton.addActionListener(nullifyListener);

        JPanel buttonPane = new JPanel();
        buttonPane.add(okButton);
        buttonPane.add(cancelButton);
        buttonPane.add(resetButton);
        dialog.add(buttonPane, BorderLayout.SOUTH);

        dialog.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                cancelListener.actionPerformed(null);
                dialog.setVisible(false);
            }
        });

        dialog.getRootPane().setDefaultButton(okButton);
        dialog.pack();

        chooserDialog = dialog;
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
