function interpreter() {
    return {
        render: function (element) {
            let div = document.createElement('div');
            //"lsfWorkerType" option is either put first when the editor is created, or put after it is created, but you have to update the mode. lsfWorkerType: script, action, form
            var aceEditor = ace.edit(div, {
                enableLiveAutocompletion: true,
                showPrintMargin: false
            });

            div.style.width = "100%";
            div.style.height = "100%";
            element.appendChild(div);
            element.aceEditor = aceEditor;

            aceEditor.container.addEventListener('keydown', function (e) {
                // disable propagation enter key
                if (e.keyCode === 13 || e.which === 13)
                    e.stopPropagation();

                // ctrl + c fix
                if (e.ctrlKey && e.keyCode === 67)
                    navigator.clipboard.writeText(aceEditor.getSelectedText());
            });

            // ctrl + v fix
            aceEditor.container.addEventListener('paste', function (e) {
                e.stopPropagation();
                e.preventDefault();
            });
        },
        update: function (element, controller, value) {
            let aceEditor = element.aceEditor;
            if (aceEditor != null) {
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
}