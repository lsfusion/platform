package lsfusion.client.descriptor.increment.editor;

import lsfusion.base.BaseUtils;
import lsfusion.base.context.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

public class IncrementColorEditor extends JPanel implements IncrementView{
    private final Object object;
    private final String field;
    public Color selected;
    public ColorLabel chosenColorLabel;
    public JLabel title = new JLabel();

    public IncrementColorEditor(String title, ApplicationContextProvider object, String field){
        this.title.setText(title);
        this.object = object;
        this.field = field;
        object.getContext().addDependency(object, field, this);
        fill();
    }

    protected IncrementColorEditor getThis() {
		return this;
	}

    public void fill() {
        chosenColorLabel = new ColorLabel(selected);
        setLayout(new BorderLayout());
        JPanel panel = new JPanel();
        panel.add(title);
        panel.add(chosenColorLabel);
        add(panel, BorderLayout.WEST);
    }

    private class ColorLabel extends JLabel implements MouseListener{
		public ColorLabel(Color color) {
			super("", SwingConstants.CENTER);
			Dimension dim = new Dimension(50, 20);
			setPreferredSize(dim);
			setMinimumSize(dim);
			setMaximumSize(dim);
			setOpaque(true);
			setBorder(BorderFactory.createLineBorder(Color.black));
			setBackground(color);
            addMouseListener(this);
		}

		@Override
		public void setBackground(Color bg) {
			super.setBackground(bg);
			if (bg == null) {
				setText("X");
			} else {
				setText("");
			}
		}

        public void mouseEntered(MouseEvent evt) {
		}

		public void mouseExited(MouseEvent evt) {
		}

		public void mousePressed(MouseEvent evt) {
		}

		public void mouseClicked(MouseEvent evt) {
            Component parent = getThis().getParent();
				if (parent == null) {
					parent = getThis();
				}
                Color color = JColorChooser.showDialog(parent, "",
						chosenColorLabel.getBackground());
				if (color != null) {
					chosenColorLabel.setBackground(color);
                    selected = color;
				}

                updateField();
		}

		public void mouseReleased(MouseEvent e) {
		}
	}
    
    private void updateField() {
        BaseUtils.invokeSetter(object, field, selected);
    }

    public void update(Object updateObject, String updateField) {
        selected = (Color) BaseUtils.invokeGetter(object, field);
    }
}
