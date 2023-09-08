package lsfusion.gwt.client.form.property.cell.classes.view;

import com.google.gwt.dom.client.Element;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.PValue;
import lsfusion.gwt.client.form.property.cell.view.UpdateContext;

public class RichTextCellRenderer extends TextCellRenderer {


    public RichTextCellRenderer(GPropertyDraw property) {
        super(property);
    }

    @Override
    public boolean updateContent(Element element, PValue value, Object extraValue, UpdateContext updateContext) {
        String innerText = value != null ? format(value) : null;

        element.setTitle(innerText);
        initQuill(element, innerText);

        return true;
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
            quill.root.innerHTML = innerText.includes('<div') ? innerText.replaceAll('<div', '<p').replaceAll('</div>', '</p>') : innerText;

        element.quill = quill;

        // quill editor bubble theme does not support opening links from edit mode.
        // https://github.com/quilljs/quill/issues/857
        // open links programmatically on ctrl+click
        quill.on('text-change', function() {
            var links = quill.root.getElementsByTagName('a');
            for (var i = 0; i < links.length; i++) {
                var link = links[i];
                if (link.onclick == null) {
                    link.onclick = function (e) {
                        if (e.ctrlKey)
                            window.open(this.href, "_blank");
                    }
                }
            }
        });

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
