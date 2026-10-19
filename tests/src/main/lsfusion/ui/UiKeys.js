// UiKeysIT's React views of UiKeys.lsf. Each root takes the focus (tabIndex): a key goes to the element that has it,
// and only a key pressed inside a form reaches the form's bindings
(function () {
    var e = React.createElement;

    function names(group) {
        return group.list.map(function (row) { return row.uiKeysName.value; }).join(', ');
    }

    // the rows as cards, a click making a row the current one
    function cards(group, controller, content) {
        return group.list.map(function (row) {
            return e('div', {key: row.key, className: 'ui-keys-card' + (row.isCurrent ? ' ui-keys-current' : ''),
                    onClick: function () { controller.i.change(row); }},
                e('span', null, row.uiKeysName.value), content && content(row));
        });
    }

    // what a call of a member threw, or that it threw nothing
    function useCall() {
        var said = React.useState(null);
        return [said[0], function (call) {
            try {
                call();
                said[1]('called');
            } catch (error) {
                said[1](error.message);
            }
        }];
    }

    // the rows, and the filter group placed by its name - its own view, or the platform's widget -; whether the filter
    // group's entry says it is hidden
    window.UiKeysBoard = function (props) {
        var data = props.data, hidden = data['FILTERGROUP(uiKeysKind)'].hidden === true;
        return e('div', {className: 'ui-keys-board', tabIndex: 0, 'data-kind-hidden': String(hidden)},
            e(window.lsfusion.Platform, {name: 'FILTERGROUP(uiKeysKind)'}),
            e('div', {className: 'ui-keys-rows'}, names(data.i)));
    };

    // the filter group's own view: its filters, the one selected marked
    window.UiKeysKindTabs = function (props) {
        var kind = props.data.i.uiKeysKind;
        return e('div', null, kind.filters.map(function (filter, index) {
            return e('span', {key: index, className: index === kind.value ? 'ui-keys-selected' : null}, filter);
        }));
    };

    // a panel action the view draws, the button of another the platform draws placed by its name, and the rows, which
    // carry their actions
    window.UiKeysActionBoard = function (props) {
        var controller = props.controller;
        return e('div', {className: 'ui-keys-actions', tabIndex: 0},
            e('button', {onClick: function () { controller.uiKeysBump.exec(); }}, 'Bump'),
            e(window.lsfusion.Platform, {name: 'uiKeysBumpPlatform'}),
            cards(props.data.i, controller));
    };

    // the rows, the form's toolbar drawn by the view, which draws no button of it
    window.UiKeysToolbarBoard = function (props) {
        return e('div', {className: 'ui-keys-toolbar', tabIndex: 0}, names(props.data.i));
    };

    // the barcode as the view's own field, changed by change(value) on ENTER, and shown as the form has it; a button
    // calling its exec()
    window.UiKeysScanBoard = function (props) {
        var controller = props.controller, call = useCall();
        return e('div', {className: 'ui-keys-scan', tabIndex: 0},
            e('input', {className: 'ui-keys-barcode', onKeyDown: function (event) {
                if (event.key === 'Enter')
                    controller.uiKeysBarcode.change(event.target.value);
            }}),
            e('span', {className: 'ui-keys-value'}, props.data.uiKeysBarcode.value),
            e('button', {className: 'ui-keys-exec', onClick: function () {
                call[1](function () { controller.uiKeysBarcode.exec(); });
            }}, 'Exec'),
            e('span', {className: 'ui-keys-said'}, call[0]));
    };

    // the rows as cards, the platform's editor of the quantity placed in each by its name and its row
    window.UiKeysCards = function (props) {
        return e('div', {className: 'ui-keys-cards', tabIndex: 0}, cards(props.data.i, props.controller, function (row) {
            return e(window.lsfusion.Platform, {name: 'i.uiKeysQty', row: row});
        }));
    };

    // what the actions asking a value wrote in the form's log; a button of each calling its exec()
    window.UiKeysAskBoard = function (props) {
        var controller = props.controller, call = useCall();
        function button(name) {
            return e('button', {className: 'ui-keys-exec-' + name, onClick: function () {
                call[1](function () { controller[name].exec(); });
            }}, name);
        }
        return e('div', {className: 'ui-keys-ask', tabIndex: 0},
            button('uiKeysAsk'), button('uiKeysAskLater'),
            e('span', {className: 'ui-keys-log'}, props.data.uiKeysAskLog.value),
            e('span', {className: 'ui-keys-said'}, call[0]));
    };
})();
