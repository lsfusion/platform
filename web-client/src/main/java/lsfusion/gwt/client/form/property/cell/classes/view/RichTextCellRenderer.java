package lsfusion.gwt.client.form.property.cell.classes.view;

import com.google.gwt.dom.client.Element;
import lsfusion.gwt.client.form.property.GPropertyDraw;

public class RichTextCellRenderer extends StringBasedCellRenderer{
    private final GPropertyDraw property;

    public RichTextCellRenderer(GPropertyDraw property) {
        super(property);
        this.property = property;
    }

    @Override
    protected void setInnerContent(Element element, String innerText) {
        initQuill(element, innerText);
    }

    @Override
    public String format(Object value) {
        return (String) value;
    }

    protected native void initQuill(Element element, String innerText)/*-{
        var toolbarOptions = [
            ['bold', 'italic', 'underline', 'strike'],        // toggled buttons
            ['link', 'image'],
            ['blockquote', 'code-block'],
            [{ 'header': 1 }, { 'header': 2 }],               // custom button values
            [{ 'list': 'ordered'}, { 'list': 'bullet' }],
            [{ 'script': 'sub'}, { 'script': 'super' }],      // superscript/subscript
            [{ 'indent': '-1'}, { 'indent': '+1' }]           // outdent/indent
                [{ 'color': [] }, { 'background': [] }],          // dropdown with defaults from theme
            [{ 'align': [] }],
            ['clean']                                         // remove formatting button
        ];

        var quill = new $wnd.Quill(element, {
            modules: {
                toolbar: toolbarOptions
            },
            bounds: element,
            theme: 'bubble',
            readOnly: true
        });

        //prevent adding newline by shift+enter commit editing
        //https://stackoverflow.com/questions/32495936/quill-js-how-to-prevent-newline-entry-on-enter-to-submit-the-input
        delete quill.getModule('keyboard').bindings[13];

        if (innerText != null)
            quill.root.innerHTML = innerText;

        element.quill = quill;
    }-*/;
}
