package lsfusion.gwt.client.form.property.cell.classes.view;

import com.google.gwt.dom.client.Element;
import lsfusion.gwt.client.form.property.GPropertyDraw;

public class RichTextCellRenderer extends StringBasedCellRenderer{

    public RichTextCellRenderer(GPropertyDraw property) {
        super(property);
    }

    @Override
    protected void setInnerContent(Element element, String innerText) {
        initQuill(element, innerText);
    }

    @Override
    public String format(Object value) {
        return (String) value;
    }

    @Override
    protected boolean needToRenderToolbarContent() {
        return false; // we don't want to render toolbar content, since richtextcelleditor is usually used for editing, and toolbar will not work / overlap and thus interfere with the editing process
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
            bounds: element, //for the tooltip is not hidden behind the parent component
            theme: 'bubble',
            readOnly: true
        });

        if (innerText != null)
            quill.root.innerHTML = innerText;

        element.quill = quill;
    }-*/;

    @Override
    public boolean isCustomRenderer() {
        return true;
    }
}
