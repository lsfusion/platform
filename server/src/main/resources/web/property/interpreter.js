function interpreter() {
    return {
        render: function (element) {
            // we need to wrap editor to easily clear element afterwards (since ace modifies it)
            // plus it seems that added keydown and paste handlers are overrided / ignored with gwt element handlers
            let editorElement = document.createElement('div');
            editorElement.style.width = "100%";
            editorElement.style.height = "100%";
            element.appendChild(editorElement);

            //"lsfWorkerType" option is either put first when the editor is created, or put after it is created, but you have to update the mode. lsfWorkerType: script, action, form
            var aceEditor = ace.edit(editorElement, {
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

            aceEditor.onBlur = function (e) {
                //need setting $isFocused to false because we "override" onBlur, but ace used this variable in inner events handlers
                aceEditor.$isFocused = false;
                //disable text caret cursor blinking
                aceEditor.renderer.hideCursor();

                let editorValue = aceEditor.getValue();
                if ((e.relatedTarget == null || !e.relatedTarget.contains(aceEditor.container)) && element.currentValue !== editorValue) {
                    element.controller.change(JSON.stringify({"text": editorValue}), oldValue => replaceField(oldValue, "text", editorValue));

                    element.currentValue = editorValue;
                }
            }
        },
        update: function (element, controller, value) {
            if (element.controller == null)
                element.controller = controller;

            let aceEditor = element.aceEditor;
            let theme = aceEditor.getOption('theme');
            let colorThemeName = controller.getColorThemeName();
            if ((colorThemeName === 'LIGHT' && theme !== 'ace/theme/chrome') || (colorThemeName === 'DARK' && theme !== 'ace/theme/ambiance'))
                aceEditor.setOption('theme', colorThemeName === 'LIGHT' ? 'ace/theme/chrome' : 'ace/theme/ambiance');

            if (value != null) {
                //updating "lsfWorkerType" option will only work after a mode change
                if (value.type !== 'java') {
                    aceEditor.session.setOption('lsfWorkerType', value.type);

                    aceEditor.session.setMode({
                        path: 'ace/mode/lsf'
                    });
                } else {
                    aceEditor.session.setMode({
                        path: 'ace/mode/java'
                    });
                }

                let editorValue = value.text;
                let currentEditorValue = aceEditor.getValue();
                // first check means that there is no editing is done (because we don't have any start / end editing here)
                if (currentEditorValue === element.currentValue && editorValue !== currentEditorValue) {
                    aceEditor.setValue(editorValue);

                    element.currentValue = editorValue;
                }
            }
        }
    }
}