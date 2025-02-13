function inputQuill(json) {
    return {
        render: function (element) {
            var inputEditor = document.createElement("div");
            inputEditor.classList.add("quill-input-editor");

            element.inputEditor = inputEditor;
            element.appendChild(inputEditor);

            var toolbarOptions = [
              ['bold', 'italic', 'underline', 'strike'],        // toggled buttons
              ['link', 'image'],
              ['blockquote', 'code-block'],

              [{ 'header': 1 }, { 'header': 2 }],               // custom button values
              [{ 'list': 'ordered'}, { 'list': 'bullet' }],
              [{ 'script': 'sub'}, { 'script': 'super' }],      // superscript/subscript
              [{ 'indent': '-1'}, { 'indent': '+1' }],           // outdent/indent

              [{ 'color': [] }, { 'background': [] }],          // dropdown with defaults from theme
              [{ 'align': [] }],

              ['clean']                                         // remove formatting button
            ];

            var quill = new Quill(inputEditor, {
                modules: {
                    toolbar: toolbarOptions
                },
                theme: 'bubble',
                bounds: inputEditor
            });

            inputEditor.onmousedown = function(event) {
                event.stopPropagation();
            }

            inputEditor.onkeydown = function(event) {
                if (event.keyCode === 13 && !event.ctrlKey && !event.shiftKey && !event.altKey)
                    event.stopPropagation();
            }

            inputEditor.onpaste = function(event) {
                if (!event.clipboardData.files.length > 0)
                    event.stopPropagation();
            }

            element.quill = quill;
        },
        update: function (element, controller, value) {
            if (value !== element.quill.root.innerHTML)
                element.quill.root.innerHTML = value;

            element.quill.root.onblur = function (event) {
                if (! (event.relatedTarget && element.inputEditor.contains(event.relatedTarget)) &&
                    value !== element.quill.root.innerHTML)
                    controller.change(json ? { action : 'change', value : element.quill.root.innerHTML } : element.quill.root.innerHTML);
            }
        }
    }
}