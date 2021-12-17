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
              [{ 'indent': '-1'}, { 'indent': '+1' }]           // outdent/indent

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
                    controller.changeValue(json ? JSON.stringify({ action : 'change', value : element.quill.root.innerHTML }) : element.quill.root.innerHTML);
            }
        }
    }
}

function uploadFiles(items, controller, event) {
    if (!items) return;

    var reader = new FileReader();
    var files = [];
    for (var i = 0; i < items.length; i++)
        if (items[i].kind === 'file')
            files.push(items[i].getAsFile());

    function readFile(index) {
        if (index >= files.length) return;
        var file = files[index];
        reader.onload = function(e) {
            let encoded = e.target.result.toString().replace(/^data:(.*,)?/, '');
            if ((encoded.length % 4) > 0) {
               encoded += '='.repeat(4 - (encoded.length % 4));
            }

            if (file.name !== 'image.png')
                controller.changeValue(JSON.stringify({ action : 'upload', name : file.name, data : encoded }));

            readFile(index+1);
        }
        reader.readAsDataURL(file);
    }
    readFile(0);
}

function inputQuillFiles() {
    var inputQuill = window.inputQuill(true);
    return {
        render: function (element) {
            inputQuill.render(element);
        },
        update: function (element, controller, value) {
            inputQuill.update(element, controller, value);

            element.quill.root.onpaste = function(event) {
                event.stopPropagation();

                uploadFiles((event.clipboardData || event.originalEvent.clipboardData).items, controller, event);
            }

            element.quill.root.ondrop = function (event) {
                if (event.dataTransfer.files.length > 0) {
                    event.preventDefault();

                    uploadFiles(event.dataTransfer.items, controller, event);
                }
            }
        }
    }
}