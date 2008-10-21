package platformlocal;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.text.Format;
import java.text.NumberFormat;
import java.text.DateFormat;
import java.awt.*;

public interface PropertyRendererComponent {

    JComponent getComponent();

    void setValue(Object value, boolean isSelected, boolean hasFocus);

}

class LabelPropertyRenderer extends JLabel { //DefaultTableCellRenderer {

    Format format = null;

    LabelPropertyRenderer(Format iformat) {
        super();

        format = iformat;
        setBorder(new EmptyBorder(1, 3, 2, 2));
        setOpaque(true);
    }

    void setSelected(boolean isSelected, boolean hasFocus) {
        if (isSelected) {
            if (hasFocus)
                setBackground(new Color(128,128,255));
            else
                setBackground(new Color(192,192,255));

        } else
            setBackground(Color.white);
    }

}


class IntegerPropertyRenderer extends LabelPropertyRenderer
                              implements PropertyRendererComponent{

    public IntegerPropertyRenderer(Format iformat) {
        super((iformat == null) ? NumberFormat.getInstance() : iformat);

        setHorizontalAlignment(JLabel.RIGHT);

    }

    public JComponent getComponent() {
        return this;
    }

    public void setValue(Object value, boolean isSelected, boolean hasFocus) {
        if (value != null)
            setText(format.format(value));
        else
            setText("");
        setSelected(isSelected, hasFocus);
    }


}
class StringPropertyRenderer extends LabelPropertyRenderer
                             implements PropertyRendererComponent {

    public StringPropertyRenderer(Format iformat) {
        super(iformat);

//        setHorizontalAlignment(JLabel.LEFT);

    }

    public JComponent getComponent() {
        return this;
    }

    public void setValue(Object value, boolean isSelected, boolean hasFocus) {
        if (value != null)
            setText(value.toString());
        else
            setText("");
        setSelected(isSelected, hasFocus);
    }

}

class DatePropertyRenderer extends LabelPropertyRenderer
                           implements PropertyRendererComponent {

    public DatePropertyRenderer(Format iformat) {
        super((iformat == null) ? DateFormat.getDateInstance() : iformat);

        setHorizontalAlignment(JLabel.RIGHT);

    }

    public JComponent getComponent() {
        return this;
    }

    public void setValue(Object value, boolean isSelected, boolean hasFocus) {
        if (value != null)
            setText(format.format(DateConverter.intToDate((Integer)value)));
        else
            setText("");
        setSelected(isSelected, hasFocus);
    }

}

class BitPropertyRenderer extends JCheckBox
                          implements PropertyRendererComponent {

    public BitPropertyRenderer() {
        super();

        setHorizontalAlignment(JCheckBox.CENTER);

        setOpaque(true);
    }

    public JComponent getComponent() {
        return this;
    }

    public void setValue(Object value, boolean isSelected, boolean hasFocus) {
        if (value != null)
            setSelected((Integer)value != 0);
        else
            setSelected(false);

        if (isSelected) {
            if (hasFocus)
                setBackground(new Color(128,128,255));
            else
                setBackground(new Color(192,192,255));

        } else
            setBackground(Color.white);
        
        if (!hasFocus && value == null) {
            this.setBackground(Color.lightGray);
        }
    }
}
