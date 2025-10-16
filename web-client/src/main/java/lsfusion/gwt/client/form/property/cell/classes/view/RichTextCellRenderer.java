package lsfusion.gwt.client.form.property.cell.classes.view;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.dom.client.Element;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.classes.data.GJSONType;
import lsfusion.gwt.client.form.object.table.grid.view.GSimpleStateTableView;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.PValue;
import lsfusion.gwt.client.form.property.cell.view.CellRenderer;
import lsfusion.gwt.client.form.property.cell.view.RenderContext;
import lsfusion.gwt.client.form.property.cell.view.RendererType;
import lsfusion.gwt.client.form.property.cell.view.UpdateContext;

public class RichTextCellRenderer extends CellRenderer {


    public RichTextCellRenderer(GPropertyDraw property) {
        super(property);
    }

    @Override
    public boolean renderContent(Element element, RenderContext renderContext) {
        renderQuill(element, null);

        if (renderContext.isInputRemoveAllPMB())
            CellRenderer.removeAllPMB(null, element);

        GwtClientUtils.addClassName(element, "form-control");
        return true;
    }

    @Override
    public boolean updateContent(Element element, PValue value, Object extraValue, UpdateContext updateContext) {
        String innerText = value != null ? format(value, updateContext.getRendererType(), updateContext.getPattern()) : "";

        element.setTitle(innerText);
        update(element, innerText, GSimpleStateTableView.convertToJSValue(GJSONType.instance, null, false, updateContext.getPropertyCustomOptions()));
        return true;
    }

    @Override
    public boolean clearRenderContent(Element element, RenderContext renderContext) {
        destroy(element);
        return true;
    }

    @Override
    public String format(PValue value, RendererType rendererType, String pattern) {
        return PValue.getStringValue(value);
    }

    protected native void renderQuill(Element element, JavaScriptObject options)/*-{
        var Quill = $wnd.Quill;
        var thisObj = this;

        changeQuillBlotTagName('formats/bold', 'B'); // Quill uses <strong> by default
        changeQuillBlotTagName('formats/italic', 'I'); // Quill uses <em> by default

        var quillParent = element;
        // need a wrapper element because if the snow theme is used, the quill replaces the element
        if (options != null && options.theme != null && options.theme === 'snow') {
            quillParent = document.createElement('div');
            element.appendChild(quillParent);
            element.classList.add('ql-snow-wrapper')
        }

        var config = @RichTextCellRenderer::getConfig(*)(quillParent, options);
        var quill = new Quill(quillParent, config);
        element.config = config;
        element.quill = quill;
        element.quillParent = quillParent;

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

        quill.on('text-change', function(delta) {
            if (delta.ops && delta.ops.length > 0) {
                delta.ops.forEach(function(op) {
                    if (op.insert && op.insert.image) {// check if image inserted
                        renderer.@RichTextCellRenderer::disableSuppressBlur()();
                    }
                });
            }
        });

        if (thisObj.@CellRenderer::property.@GPropertyDraw::hasAutoHeight()())
            element.getElementsByClassName("ql-editor")[0].classList.add("auto-sized-y")

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

    protected native void update(Element element, String innerText, JavaScriptObject options)/*-{
        var thisObj = this;
        var config = @RichTextCellRenderer::getConfig(*)(element, options);
        if (!$wnd.deepEquals(element.config, config)) {
            @RichTextCellRenderer::destroy(*)(element);
            thisObj.@RichTextCellRenderer::renderQuill(*)(element, config);
            element.config = config;
        }

        if (innerText != null)
            element.quill.root.innerHTML = innerText.includes('<div') ? innerText.replaceAll('<div', '<p').replaceAll('</div>', '</p>') : innerText;
    }-*/;

    protected native static JavaScriptObject getConfig(Element quillParent, JavaScriptObject options)/*-{
        return $wnd.mergeObjects({
            modules: {
                toolbar: [
                    ['bold', 'italic', 'underline', 'strike'],        // toggled buttons
                    ['link', 'image'],
                    ['blockquote', 'code-block'],
                    [{ 'header': 1 }, { 'header': 2 }],               // custom button values
                    [{ 'list': 'ordered'}, { 'list': 'bullet' }],
                    [{ 'script': 'sub'}, { 'script': 'super' }],      // superscript/subscript
                    [{ 'indent': '-1'}, { 'indent': '+1' }],          // outdent/indent
                    [{ 'color': [] }, { 'background': [] }],          // dropdown with defaults from theme
                    [{ 'align': [] }],
                    ['clean']                                         // remove formatting button
                ]
            },
            bounds: quillParent, //for the tooltip is not hidden behind the parent component
            theme: 'bubble',
            readOnly: true
        }, options);
    }-*/;

    protected native static void destroy(Element element)/*-{
        if (element.quill != null) {
            if (element.quillParent != null)
                element.quillParent.innerHTML = '';
            else
                element.innerHTML = '';

            element.quill = null;

            // We need to remove all CSS classes associated with Quill
            // because when updating the config and changing theme from bubble to snow,
            // quillParent also changes, which makes some buttons in toolbar not work.
            element.classList.remove(
                'ql-bubble',
                'ql-snow',
                'ql-toolbar',
                'ql-container',
                'ql-editor',
                'ql-disabled'
            );
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
