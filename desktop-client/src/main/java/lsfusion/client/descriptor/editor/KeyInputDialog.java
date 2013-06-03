package lsfusion.client.descriptor.editor;

import lsfusion.client.ClientResourceBundle;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class KeyInputDialog extends JDialog implements ActionListener {
        JTextField keyText = new JTextField(20);
        JButton okBut = new JButton();
        JButton cancelBut = new JButton();
        String resultString = "";
        String keyStrokeString = "";
        boolean ok;

        public KeyInputDialog(JFrame owner){
            super(owner, ClientResourceBundle.getString("descriptor.editor.shortcuts"), true);
            setSize(270, 130);
            setResizable(false);
            addWindowListener(new WindowListener());
            setLocationRelativeTo(owner);

            okBut.setText(ClientResourceBundle.getString("descriptor.editor.okbutton"));
            okBut.addActionListener(this);
            cancelBut.setText(ClientResourceBundle.getString("descriptor.editor.cancelbutton"));
            cancelBut.addActionListener(this);
            keyText.addKeyListener(new KeyListener(){
                public void keyPressed(KeyEvent e){
                    KeyStroke kst = KeyStroke.getKeyStrokeForEvent(e);
                    keyStrokeString = kst.toString();
                    update();
                }
                public void keyReleased(KeyEvent e){
                    keyText.setText(resultString);
                }
                public void keyTyped(KeyEvent e){
                }
            });

            JPanel inputPanel = new JPanel();
            inputPanel.add(new JLabel(ClientResourceBundle.getString("descriptor.editor.press.key.combination")));

            JPanel buts = new JPanel();
            buts.add(okBut);
            buts.add(cancelBut);

            JPanel resPanel = new JPanel();
            resPanel.add(keyText);

            setLayout(new GridLayout(3, 1));
            add(inputPanel);
            add(resPanel);
            add(buts);
        }

        public String showDialog(){
            setVisible(true);
            if(ok){
                return keyStrokeString;
            }
            else {
                return null;
            }
        }

        public void actionPerformed(ActionEvent e){
            update();
            if(e.getSource() == okBut) {
                ok = true;
                setVisible(false);
            }
            if(e.getSource() == cancelBut){
                ok = false;
                setVisible(false);
            }
            keyText.requestFocusInWindow();
        }

        class WindowListener extends WindowAdapter {
            public void windowClosed(WindowEvent e) {
                ok = false;
            }
        }

        private void update(){
            resultString = "";
            if(keyStrokeString.contains("ctrl")) {
                resultString += "Ctrl + ";
            }
            if(keyStrokeString.contains("alt")) {
                resultString += "Alt + ";
            }
            if(keyStrokeString.contains("shift")) {
                resultString += "Shift + ";
            }
            String button = keyStrokeString.substring(keyStrokeString.lastIndexOf(' ') + 1);
            if(!button.equals("ALT") && !button.equals("CONTROL") && !button.equals("SHIFT")){
                resultString += button;
            }
            else {
                resultString = resultString.substring(0, resultString.length() - 3);
            }
            keyText.setText(resultString);
        }
    }