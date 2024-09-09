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
        String innerText = value != null ? format(value, updateContext.getRendererType(), updateContext.getPattern()) : "";

        element.setTitle(innerText);
        initQuill(element, innerText, property.valueHeight == -1);

        return true;
    }

    protected native void initQuill(Element element, String innerText, boolean autoSizedY)/*-{
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

        //The image selection dialog triggers a blur event, which ends the editor editing.
        // Need to enable suppressBlur before image dialog and then disable it
        // either after inserting the image or after canceling the insertion
        // (when the user closes the image selection dialog or clicks the cancel button).
        var renderer = this;
        var toolbar = quill.getModule('toolbar');
        toolbar.addHandler("image", function () {
            renderer.@RichTextCellRenderer::enableSuppressBlur()();
            toolbar.options.handlers.image.call(toolbar);

            var inputElement = toolbar.container.querySelector("input");
            inputElement.oncancel = function () {
                renderer.@RichTextCellRenderer::disableSuppressBlur()();
            }
        })

        quill.on('text-change', function(delta, oldDelta, source) {
            if (delta.ops && delta.ops.length > 0) {
                delta.ops.forEach(function(op) {
                    if (op.insert && op.insert.image) {// check if image inserted
                        renderer.@RichTextCellRenderer::disableSuppressBlur()();
                    }
                });
            }
        });

        if (innerText != null)
            quill.root.innerHTML = innerText.includes('<div') ? innerText.replaceAll('<div', '<p').replaceAll('</div>', '</p>') : innerText;

        element.quill = quill;

        if (autoSizedY) {
            element.getElementsByClassName("ql-editor")[0].classList.add("auto-sized-y")
        }
        
        // quill editor bubble theme does not support opening links from edit mode.
        // https://github.com/quilljs/quill/issues/857
        // open links programmatically on ctrl+click
        // text-change event is triggered by RichTextCellEditor.enableEditing method and every time the text changes
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

    private boolean fileDialog = false;
    private native void enableSuppressBlur()/*-{
        this.@RichTextCellRenderer::fileDialog = true;
        @lsfusion.gwt.client.base.FocusUtils::enableSuppressBlur()();
    }-*/;

    private native void disableSuppressBlur()/*-{
        if(this.@RichTextCellRenderer::fileDialog) {
            this.@RichTextCellRenderer::fileDialog = false;
            @lsfusion.gwt.client.base.FocusUtils::disableSuppressBlur()();
        }
    }-*/;


    @Override
    public boolean isCustomRenderer() {
        return true;
    }
}
