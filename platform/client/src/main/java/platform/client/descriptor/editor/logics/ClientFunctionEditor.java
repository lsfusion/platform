package platform.client.descriptor.editor.logics;

import platform.client.descriptor.editor.base.TitledPanel;
import platform.client.descriptor.increment.editor.IncrementTextEditor;
import platform.client.logics.ClientFunction;

import javax.swing.*;
import java.awt.*;

public class ClientFunctionEditor extends ClientComponentEditor {
    public ClientFunctionEditor(ClientFunction function) {
        super("Функциональный компонент", function);

        add(new TitledPanel("Заголовок", new IncrementTextEditor(function, "caption")));
        add(Box.createRigidArea(new Dimension(5, 5)));
    }
}
