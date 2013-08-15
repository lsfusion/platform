package lsfusion.client.descriptor.increment.editor;

import lsfusion.base.BaseUtils;
import lsfusion.client.descriptor.editor.FontChooser;
import lsfusion.base.context.*;
import lsfusion.client.descriptor.editor.base.FlatButton;
import lsfusion.client.descriptor.editor.base.TitledPanel;

import java.awt.*;
import java.util.Map;

public class IncrementFontEditor extends TitledPanel implements IncrementView {
    private final Object object;
    private final String field;
    private Font font;
    FontFlatButton fontButton = new FontFlatButton();

    public IncrementFontEditor(String title, ApplicationContextProvider object, String field) {
        super(title);
        this.object = object;
        this.field = field;
        object.getContext().addDependency(object, field, this);

        add(fontButton);
        if (font != null) {
            fontButton.retypeText();
        }
    }

    private void reValidate() {
        revalidate();
    }

    private void updateField() {
        BaseUtils.invokeSetter(object, field, font);
    }

    public void update(Object updateObject, String updateField) {
        font = (Font) BaseUtils.invokeGetter(object, field);
    }

    private class FontFlatButton extends FlatButton {

        public void onClick() {
            FontChooser chooser = new FontChooser(null, font);
            if (chooser.showDialog()) {
                font = chooser.getFont();
            }
            if (font != null) {
                retypeText();
                updateField();
            }
        }

        private void retypeText() {
            setText("AaBbCc АаБбВв 123");

            Map attributes = font.getAttributes();
            Font tempFont = new Font(font.getFontName(), font.getStyle(), 0).deriveFont(attributes);
            setFont(tempFont);

            reValidate();
        }
    }
}
