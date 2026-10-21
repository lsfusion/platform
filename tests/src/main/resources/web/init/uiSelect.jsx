// the React views of UiSelect.lsf, which UiSelectIT drives in the browser - no build: the server transforms them when it
// serves them

// a React view placing what the platform draws: the panel's selections and, per row, the row's
function UiSelectPlaced(props) {
    const { Platform } = window.lsfusion;
    return <div>
        <div className="ui-select-panel"><Platform name="o.panelStatus"/></div>
        <div className="ui-select-fn"><Platform name="o.fnStatus"/></div>
        {props.data.o.list.map(row => <div key={row.key} className="ui-select-row"><Platform name="o.placedStatus" row={row}/></div>)}
    </div>;
}

// a render function given in CUSTOM with SELECT: it draws the selection, given the options as the platform's element is
// - its value the list [{name, objects, selected}] -, a button an option, pressed while picked, and picks as that
// element does
window.uiSelectFn = function () {
    return {
        render: function (element) {
        },
        update: function (element, controller, value) {
            element.textContent = '';
            (Array.isArray(value) ? value : []).forEach(function (option) {
                var button = document.createElement('button');
                button.textContent = 'fn ' + option.name;
                button.setAttribute('aria-pressed', String(!!option.selected));
                button.onclick = function () { controller.changeProperty('selected', option, true); };
                element.appendChild(button);
            });
        }
    };
};
