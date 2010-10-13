package platform.client.descriptor.editor.base;

import platform.client.descriptor.increment.IncrementView;

import javax.swing.*;
import java.lang.reflect.Method;
import java.util.List;

public class IncrementListEditor extends JList implements IncrementView {

    private final Object object;
    private final String field;
    public IncrementListEditor(ListModel dataModel, Object object, String field) {
        super(dataModel);

        this.object = object;
        this.field = field;
    }

    public void update(Object updateObject, String updateField) {
        clearSelection();

        try {
            Method method = object.getClass().getMethod("get"+ field);
            List<?> selected = (List<?>) method.invoke(updateObject);
//            for(Object selectObject : selected)
//                set
            
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
