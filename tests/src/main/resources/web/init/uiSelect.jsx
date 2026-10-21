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

// a React view drawing the selections from the options it is given, `select.list`, and changing them by what is picked:
// an option, its value, null - of several values the array of the options checked from now on, or their value, the
// values joined as the property has them. Every clickable has a text of its own; it hands on what it is given as it
// draws, window.uiSelectData, for the test to read
function UiSelectPicker(props) {
    const { data, controller } = props;
    const o = data.o;
    window.uiSelectData = data;
    const checkedWith = (list, option) => list.filter(each => each === option ? !each.selected : each.selected);
    return <div>
        <div>
            {o.panelStatus.select.list.map(option =>
                <button key={option.key} onClick={() => controller.o.panelStatus.change(option)}>{'ps ' + option.value}</button>)}
            <button onClick={() => controller.o.panelStatus.change('Lost')}>ps by value Lost</button>
            <button onClick={() => controller.o.panelStatus.change(null)}>ps clear</button>
        </div>
        <div>
            {o.panelTags.select.list.map(option =>
                <label key={option.key}>
                    <input type="checkbox" checked={!!option.selected}
                           onChange={() => controller.o.panelTags.change(checkedWith(o.panelTags.select.list, option))}/>
                    {'pt ' + option.value}
                </label>)}
            <button onClick={() => controller.o.panelTags.change('blue, green')}>pt value blue, green</button>
        </div>
        <div>
            {o.inputStatus.select.list.map(option =>
                <button key={option.key} onClick={() => controller.o.inputStatus.change(option.value)}>{'is ' + option.value}</button>)}
        </div>
        {o.list.map((row, i) => <div key={row.key}>
            {row.uiSelectNameStatus.select.list.map(option =>
                <button key={option.key} onClick={() => controller.o.uiSelectNameStatus.change(option, row)}>{'r' + (i + 1) + ' ' + option.value}</button>)}
            {row.uiSelectTags.select.list.map(option =>
                <label key={option.key}>
                    <input type="checkbox" checked={!!option.selected}
                           onChange={() => controller.o.uiSelectTags.change(checkedWith(row.uiSelectTags.select.list, option), row)}/>
                    {'r' + (i + 1) + ' tag ' + option.value}
                </label>)}
        </div>)}
    </div>;
}
