package platform.client.descriptor.editor;

import platform.client.ClientResourceBundle;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.font.TextAttribute;
import java.util.HashMap;
import java.util.Map;

public class FontChooser extends JDialog implements ActionListener {
    private boolean ok = false;
    private String[] fontName = GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
    private JTextField textField = new JTextField();
    private JCheckBox boldBox, italicBox, underlineBox, strikeThroughBox;
    private String[] sizeArray, widthArray;
    private JComboBox nameComboBox, sizeComboBox, widthComboBox;
    private JButton okButton, cancelButton;
    private Font font, curFont;

    public FontChooser(JFrame owner, Font curFont) {
        super(owner, ClientResourceBundle.getString("descriptor.editor.font.selection"), true);
        this.curFont = curFont;
        addWindowListener(new WindowListener());
        setVisible(false);
        setSize(370, 330);
        setResizable(false);
        setLocationRelativeTo(owner);
        fillFrame();
        setFont();
    }

    private void fillFrame(){
        getContentPane().setLayout(new BoxLayout(getContentPane(), BoxLayout.Y_AXIS));

        JPanel namePanel = new JPanel();
        namePanel.setLayout(new GridLayout());
        namePanel.setBorder(new CompoundBorder(new TitledBorder(ClientResourceBundle.getString("descriptor.editor.font")), new EmptyBorder(0, 0, 0, 0)));
        nameComboBox = new JComboBox(fontName);
        nameComboBox.setSelectedItem("Dialog");
        nameComboBox.addActionListener(this);
        namePanel.add(nameComboBox);

        JPanel sizePanel = new JPanel();
        sizePanel.setLayout(new GridLayout());
        sizePanel.setBorder(new CompoundBorder(new TitledBorder(ClientResourceBundle.getString("descriptor.editor.font.size")), new EmptyBorder(0, 0, 0, 0)));
        sizeComboBox = new JComboBox(getSizes());
        sizeComboBox.setSelectedItem("12");
        sizeComboBox.addActionListener(this);
        sizePanel.add(sizeComboBox);

        JPanel widthPanel = new JPanel();
        widthPanel.setLayout(new GridLayout());
        widthPanel.setBorder(new CompoundBorder(new TitledBorder(ClientResourceBundle.getString("descriptor.editor.font.width")), new EmptyBorder(0, 0, 0, 0)));
        widthComboBox = new JComboBox(getWidths());
        widthComboBox.addActionListener(this);
        widthPanel.add(widthComboBox);

        JPanel sizesPanel = new JPanel();
        sizesPanel.setLayout(new GridLayout(1, 2, 3, 3));
        sizesPanel.add(sizePanel);
        sizesPanel.add(widthPanel);

        JPanel boxPanel = new JPanel();
        boxPanel.setLayout(new GridLayout(2, 2));
        boxPanel.setBorder(new CompoundBorder(new TitledBorder(ClientResourceBundle.getString("descriptor.editor.font.style")), new EmptyBorder(0, 0, 0, 0)));
        boldBox = new JCheckBox(ClientResourceBundle.getString("descriptor.editor.font.style.bold"));
        boldBox.addActionListener(this);
        italicBox = new JCheckBox(ClientResourceBundle.getString("descriptor.editor.font.style.italic"));
        italicBox.addActionListener(this);
        underlineBox = new JCheckBox(ClientResourceBundle.getString("descriptor.editor.font.style.underlined"));
        underlineBox.addActionListener(this);
        strikeThroughBox = new JCheckBox(ClientResourceBundle.getString("descriptor.editor.font.style.strikeout"));
        strikeThroughBox.addActionListener(this);
        boxPanel.add(boldBox);
        boxPanel.add(italicBox);
        boxPanel.add(underlineBox);
        boxPanel.add(strikeThroughBox);

        JPanel textPanel = new JPanel();
        textPanel.setLayout(new GridLayout());
        textPanel.setBorder(new CompoundBorder(new TitledBorder(ClientResourceBundle.getString("descriptor.editor.font.preview")), new EmptyBorder(0, 0, 0, 0)));
        textPanel.setPreferredSize(new Dimension(getWidth(), 200));
        textField.setEditable(false);
        textField.setHorizontalAlignment(JTextField.CENTER);
        textField.setText("AaBbCc АаБбВв 123");
        textPanel.add(textField);

        JPanel butPanel = new JPanel();
        butPanel.setLayout(new GridLayout(1, 2, 5, 5));
        okButton = new JButton(ClientResourceBundle.getString("descriptor.editor.okbutton"));
        okButton.addActionListener(this);
        cancelButton = new JButton(ClientResourceBundle.getString("descriptor.editor.cancelbutton"));
        cancelButton.addActionListener(this);
        butPanel.add(okButton);
        butPanel.add(cancelButton);

        add(namePanel);
        add(Box.createRigidArea(new Dimension(5, 5)));
        add(sizesPanel);
        add(Box.createRigidArea(new Dimension(5, 5)));
        add(boxPanel);
        add(Box.createRigidArea(new Dimension(5, 5)));
        add(textPanel);
        add(Box.createRigidArea(new Dimension(5, 5)));
        add(butPanel);
    }

    private void setFont(){
        if(curFont != null){
            nameComboBox.setSelectedItem(curFont.getFamily());
            sizeComboBox.setSelectedItem(String.valueOf(curFont.getSize()));
            boldBox.setSelected(curFont.isBold());
            italicBox.setSelected(curFont.isItalic());
            underlineBox.setSelected(curFont.getAttributes().get(TextAttribute.UNDERLINE) == TextAttribute.UNDERLINE_ON);
            strikeThroughBox.setSelected(curFont.getAttributes().get(TextAttribute.STRIKETHROUGH) == TextAttribute.STRIKETHROUGH_ON);
            if(curFont.getAttributes().get(TextAttribute.WIDTH) != null){
                widthComboBox.setSelectedItem(curFont.getAttributes().get(TextAttribute.WIDTH).toString());
            }
            else {
                widthComboBox.setSelectedItem(TextAttribute.WIDTH_REGULAR.toString());
            }
            font = curFont;
            textField.setFont(font);
        }
        else{
            widthComboBox.setSelectedItem(TextAttribute.WIDTH_REGULAR.toString());
            setProperties();
        }
    }

    class WindowListener extends WindowAdapter {
        public void windowClosed(WindowEvent e) {
            ok = false;
        }
    }

    private String[] getSizes() {
        if(sizeArray == null) {
            sizeArray = new String[35];
            for(int i = 5; i < 40; i++)
                sizeArray[i - 5] = String.valueOf(i);
        }
        return sizeArray;
    }

    private String[] getWidths() {
        if(widthArray == null) {
            widthArray = new String[5];
            Float[] widthArr = new Float[]{
                    TextAttribute.WIDTH_CONDENSED,
                    TextAttribute.WIDTH_SEMI_CONDENSED,
                    TextAttribute.WIDTH_REGULAR,
                    TextAttribute.WIDTH_SEMI_EXTENDED,
                    TextAttribute.WIDTH_EXTENDED};
            for(int i = 0; i < 5; i++)
                widthArray[i] = String.valueOf(widthArr[i]);
        }
        return widthArray;
    }

    public void actionPerformed(ActionEvent e) {
        setProperties();
        if(e.getSource() == okButton) {
            ok = true;
            setVisible(false);
        }
        if(e.getSource() == cancelButton) {
            ok = false;
            setVisible(false);
        }
    }

    private void setProperties() {
        int style = 0;
        Map<TextAttribute, Object> fontAttributes = new HashMap<TextAttribute, Object>();
        if(boldBox.isSelected())
            style += Font.BOLD;
        if(italicBox.isSelected())
            style += Font.ITALIC;
        if(underlineBox.isSelected()){
            fontAttributes.put(TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_ON);
        }
        if(strikeThroughBox.isSelected()){
            fontAttributes.put(TextAttribute.STRIKETHROUGH, TextAttribute.STRIKETHROUGH_ON);
        }
        fontAttributes.put(TextAttribute.WIDTH, Float.parseFloat(widthComboBox.getSelectedItem().toString()));
        String[] size = getSizes();
        font = new Font(fontName[nameComboBox.getSelectedIndex()], style,
                Integer.parseInt(size[sizeComboBox.getSelectedIndex()])).deriveFont(fontAttributes);
        textField.setFont(font);
    }

    public boolean showDialog() {
        setVisible(true);
        return ok;
    }

    public Font getFont() {
        return font;
    }
}