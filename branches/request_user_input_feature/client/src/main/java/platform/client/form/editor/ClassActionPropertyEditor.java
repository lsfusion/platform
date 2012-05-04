package platform.client.form.editor;

import platform.client.SwingUtils;
import platform.client.form.classes.ClassDialog;
import platform.client.logics.classes.ClientObjectClass;

import java.awt.*;

public class ClassActionPropertyEditor extends DialogBasedPropertyEditor {
    private ClassDialog dialog;

    public ClassActionPropertyEditor(Component owner, ClientObjectClass baseClass, ClientObjectClass value) {
        dialog = new ClassDialog(owner, baseClass, value, true);
    }

    @Override
    public void showDialog(Point desiredLocation) {
        SwingUtils.requestLocation(dialog, desiredLocation);
        dialog.setVisible(true);
    }

    public boolean valueChanged() {
        return dialog.getChosenClass() != null;
    }

    public Object getCellEditorValue() {
        Object value = dialog.getChosenClass();
        if (value instanceof ClientObjectClass) {
            return ((ClientObjectClass) value).ID; // приходится так извращаться, так как передавать надо не Class, а ID
        } else {
            return null;
        }
    }
}
