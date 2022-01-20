function interpreter() {
    return {
        render: function (element) {
            //"lsfWorkerType" option is either put first when the editor is created, or put after it is created, but you have to update the mode. lsfWorkerType: script, action, form
            var aceEditor = ace.edit(element, {
                enableLiveAutocompletion: true,
                showPrintMargin: false
            });

            element.aceEditor = aceEditor;

            aceEditor.container.addEventListener('keydown', function (e) {

                //when autocomplete popup shown, it is appended to the body and stays in the DOM when the editor is closed.
                let completer = aceEditor.completer;
                if (completer && completer.popup && completer.popup.container && !element.contains(completer.popup.container))
                    element.appendChild(completer.popup.container);

                // disable propagation enter key
                if (e.keyCode === 13 || e.which === 13)
                    e.stopPropagation();

                // ctrl + c fix
                if (e.ctrlKey && e.keyCode === 67) {
                    let textToCopy = aceEditor.getSelectedText();
                    //fix from https://stackoverflow.com/questions/51805395/navigator-clipboard-is-undefined
                    // navigator clipboard api needs a secure context (https)
                    if (navigator.clipboard && window.isSecureContext) {
                        navigator.clipboard.writeText(textToCopy);
                    } else {
                        let textArea = document.createElement("textarea");
                        textArea.value = textToCopy;
                        // make the textarea out of viewport
                        textArea.style.position = "fixed";
                        textArea.style.left = "-999999px";
                        textArea.style.top = "-999999px";
                        document.body.appendChild(textArea);
                        textArea.focus();
                        textArea.select();

                        document.execCommand('copy');
                        textArea.remove();
                        aceEditor.focus();
                    }
                }
            });

            // ctrl + v fix
            aceEditor.container.addEventListener('paste', function (e) {
                e.stopPropagation();
                e.preventDefault();
            });
        },
        update: function (element, controller, value) {
            let aceEditor = element.aceEditor;
            let theme = aceEditor.getOption('theme');
            let colorThemeName = controller.getColorThemeName();
            if ((colorThemeName === 'LIGHT' && theme !== 'ace/theme/chrome') || (colorThemeName === 'DARK' && theme !== 'ace/theme/ambiance'))
                aceEditor.setOption('theme', colorThemeName === 'LIGHT' ? 'ace/theme/chrome' : 'ace/theme/ambiance');

            let parsedValue = JSON.parse(value);

            aceEditor.onBlur = function (e) {
                if (e.relatedTarget == null || !e.relatedTarget.contains(aceEditor.container))
                    controller.changeValue(JSON.stringify({text : aceEditor.getValue()}));
            }

            if (parsedValue != null) {
                //updating "lsfWorkerType" option will only work after a mode change
                if (parsedValue.type !== 'java') {
                    aceEditor.session.setOption('lsfWorkerType', parsedValue.type);

                    aceEditor.session.setMode({
                        path: 'ace/mode/lsf'
                    });
                } else {
                    aceEditor.session.setMode({
                        path: 'ace/mode/java'
                    });
                }

                if (parsedValue.text !== aceEditor.getValue())
                    aceEditor.setValue(parsedValue.text);
            }
        }
    }
}