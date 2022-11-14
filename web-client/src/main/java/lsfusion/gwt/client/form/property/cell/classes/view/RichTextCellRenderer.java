package lsfusion.gwt.client.form.property.cell.classes.view;

import com.google.gwt.dom.client.Element;
import lsfusion.gwt.client.form.property.GPropertyDraw;

public class RichTextCellRenderer extends TextBasedCellRenderer{

    public RichTextCellRenderer(GPropertyDraw property) {
        super(property);
    }

    @Override
    protected boolean setInnerContent(Element element, String innerText) {
        initQuill(element, innerText);
        return true;
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

        var Quill = $wnd.Quill;

        changeQuillBlotTagName('formats/bold', 'B'); // Quill uses <strong> by default
        changeQuillBlotTagName('formats/italic', 'I'); // Quill uses <em> by default

        var quill = new Quill(element, {
            modules: {
                toolbar: toolbarOptions
            },
            bounds: element, //for the tooltip is not hidden behind the parent component
            theme: 'bubble',
            readOnly: true
        });

        if (innerText != null)
            quill.root.innerHTML = innerText.includes('<div>') ? innerText.replaceAll('<div>', '<p>').replaceAll('</div>', '</p>') : innerText;

        element.quill = quill;

        //https://quilljs.com/guides/how-to-customize-quill/
        function changeQuillBlotTagName(blotName, tagName){
            var blot = Quill.imports[blotName];
            blot.tagName = tagName;
            Quill.register(blot, true);
        }
    }-*/;

    @Override
    public boolean isCustomRenderer() {
        return true;
    }
}
