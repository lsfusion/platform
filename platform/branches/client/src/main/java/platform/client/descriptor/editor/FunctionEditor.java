package platform.client.descriptor.editor;

import platform.client.descriptor.editor.base.TitledPanel;
import platform.client.descriptor.increment.editor.IncrementTextEditor;
import platform.client.logics.ClientFunction;

import javax.swing.*;
import java.awt.*;

public class FunctionEditor extends ComponentEditor {
    public FunctionEditor(ClientFunction function) {
        super("Функциональный компонент", function);

        add(new TitledPanel("Заголовок", new IncrementTextEditor(function, "caption")));
        add(Box.createRigidArea(new Dimension(5, 5)));
    }
}
