package platform.client.code;

import javax.swing.*;
import java.awt.*;

public class CodeDialog extends JDialog{
    JTextArea textArea;

    public CodeDialog(JFrame owner, String text) {
        super(owner, "Сгенерированный код");
        setSize(800, 600);
        setLocationRelativeTo(owner);

        textArea = new JTextArea(text);
        textArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        add(new JScrollPane(textArea));

        setVisible(true);
    }
}
