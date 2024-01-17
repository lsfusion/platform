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

            const i = file.name.lastIndexOf('.');

            controller.change({ name : i === -1 ? file.name : file.name.substring(0, i),
                                extension : i === -1 ? "" : file.name.substring(i + 1),
                                data : encoded });

            readFile(index+1);
        }
        reader.readAsDataURL(file);
    }
    readFile(0);
}

function inputFile() {
    return {
        render: function (element) {
        },
        update: function (element, controller, value) {
            let dropArea = element.parentElement.parentElement;

            if (dropArea != null) {
                dropArea.onpaste = function(event) {
                    event.stopPropagation();

                    uploadFiles((event.clipboardData || event.originalEvent.clipboardData).items, controller, event);
                }

                dropArea.ondragover = function (event) {
                    event.preventDefault();
                }
                dropArea.ondrop = function (event) {
                    if (event.dataTransfer.files.length > 0) {
                        event.preventDefault();

                        uploadFiles(event.dataTransfer.items, controller, event);
                    }
                }

                element.style.display = "none";
            }
        }
    }
}
