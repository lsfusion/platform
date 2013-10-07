package lsfusion.client.descriptor.increment.editor;

import lsfusion.base.ReflectionUtils;
import lsfusion.base.context.ApplicationContextProvider;
import lsfusion.base.context.IncrementView;
import lsfusion.client.descriptor.editor.FontChooser;
import lsfusion.client.descriptor.editor.base.FlatButton;
import lsfusion.client.descriptor.editor.base.TitledPanel;
import lsfusion.interop.FontInfo;

public class IncrementFontInfoEditor extends TitledPanel implements IncrementView {
    private final Object object;
    private final String field;
    private FontInfo font;

    public IncrementFontInfoEditor(String title, ApplicationContextProvider object, String field) {
        super(title);
        this.object = object;
        this.field = field;
        object.getContext().addDependency(object, field, this);

        FontFlatButton fontButton = new FontFlatButton();
        add(fontButton);
        if (font != null) {
            fontButton.retypeText();
        }
    }

    private void reValidate() {
        revalidate();
    }

    private void updateField() {
        ReflectionUtils.invokeSetter(object, field, font);
    }

    public void update(Object updateObject, String updateField) {
        font = (FontInfo) ReflectionUtils.invokeGetter(object, field);
    }

    private class FontFlatButton extends FlatButton {

        public void onClick() {
            FontChooser chooser = new FontChooser(null, font == null ? null : font.deriveFrom(this));
            if (chooser.showDialog()) {
                font = FontInfo.createFrom(chooser.getFont());
            }
            if (font != null) {
                retypeText();
                updateField();
            }
        }

        private void retypeText() {
            setText("AaBbCc АаБбВв 123");

            setFont(font.deriveFrom(this));

            reValidate();
        }
    }
}
