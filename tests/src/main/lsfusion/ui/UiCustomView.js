// UiCustomViewIT's React views of UiCustomView.lsf, its custom cell renderer, and the JS functions its INTERNAL CLIENT
// actions run

// the rows of the items, each with its name and the platform's editor of its quantity. What the first row places is the
// test's to switch, window.uiCustomViewRows.place(...): the editor once ('once'), in two places ('twice'), or in the
// second of those alone ('second')
window.UiCustomViewRows = function (props) {
    var e = React.createElement, Platform = window.lsfusion.Platform;
    var placing = React.useState('once'), place = placing[0];
    window.uiCustomViewRows = { place: placing[1] };
    function quantity(row, key) {
        return e(Platform, { key: key, name: 'i.uiCustomViewQuantity', row: row, className: 'ui-custom-view-' + key });
    }
    return e('div', { className: 'ui-custom-view-rows' }, props.data.i.list.map(function (row, index) {
        var places = index > 0 || place === 'once' ? [quantity(row, 'first')]
            : place === 'twice' ? [quantity(row, 'first'), quantity(row, 'second')]
            : [quantity(row, 'second')];
        return e('div', { key: row.key, className: 'ui-custom-view-row' },
            [e('span', { key: 'name', className: 'ui-custom-view-name' }, row.uiCustomViewName.value)].concat(places));
    }));
};

// the note the platform draws, placed in a node the view never puts in the page - the first place to render, which
// takes it - and in the page
window.UiCustomViewStray = function () {
    var e = React.createElement, Platform = window.lsfusion.Platform;
    var detached = React.useRef(null);
    if (!detached.current)
        detached.current = document.createElement('div');
    return e('div', { className: 'ui-custom-view-stray' },
        ReactDOM.createPortal(e(Platform, { name: 'uiCustomViewNote', className: 'ui-custom-view-lost' }), detached.current),
        e(Platform, { name: 'uiCustomViewNote', className: 'ui-custom-view-live' }));
};

// the rows of the items, each with its name, its quantity as the view is given it - its type and value, or none, and
// whether its change still waits for the server's answer - and a button that changes the quantity to 0 through the
// member
window.UiCustomViewQuantities = function (props) {
    var e = React.createElement, member = props.controller.i.uiCustomViewQuantity;
    return e('div', { className: 'ui-custom-view-quantities' }, props.data.i.list.map(function (row) {
        var cell = row.uiCustomViewQuantity, quantity = cell.value;
        return e('div', { key: row.key, className: 'ui-custom-view-quantity-row' },
            e('span', { className: 'ui-custom-view-name' }, row.uiCustomViewName.value),
            e('span', { className: 'ui-custom-view-quantity' },
                (quantity === null ? 'none' : typeof quantity + ' ' + quantity) + (cell.loading ? ', loading' : '')),
            e('button', { className: 'ui-custom-view-zero', onClick: function () { member.change(0, row); } }, 'zero'));
    }));
};

// buttons that call the server through the controller, each listing what its call resolved with - the type and the
// value - or was rejected with; the controller is kept as window.uiCustomViewCalls, to be called once the form is
// closed
window.UiCustomViewCalls = function (props) {
    var e = React.createElement, controller = props.controller;
    var said = React.useState([]), results = said[0], setResults = said[1];
    window.uiCustomViewCalls = controller;
    function say(result) {
        setResults(function (list) { return list.concat(result); });
    }
    function button(name, call) {
        return e('button', { key: name, className: 'ui-custom-view-' + name, onClick: function () {
            call().then(function (value) { say(name + ': ' + typeof value + ' ' + value); },
                        function (error) { say(name + ' rejected: ' + error.message); });
        } }, name);
    }
    return e('div', { className: 'ui-custom-view-calls' },
        button('eval', function () { return controller.eval('run(INTEGER a) { RETURN a * 2; }', 21); }),
        button('evalAction', function () { return controller.evalAction('UiCustomView.uiCustomViewCount() <- $1;', 5); }),
        button('read', function () { return controller.eval('run() { RETURN UiCustomView.uiCustomViewCount(); }'); }),
        button('exec', function () { return controller.exec('uiCustomViewDouble', 4); }),
        button('change', function () { return controller.change('uiCustomViewCount', 9); }),
        button('broken', function () { return controller.eval('run() { RETURN 1 +; }'); }),
        button('missing', function () { return controller.exec('uiCustomViewNoSuchAction'); }),
        e('ul', { className: 'ui-custom-view-results' }, results.map(function (result, index) {
            return e('li', { key: index }, result);
        })));
};

// what the action uiCustomViewOnClient runs: its answer is the action's, worked out on the client with the controller
// it is given at hand
window.uiCustomViewOnClient = function (a, controller) {
    return (a * 2) + (typeof controller.eval === 'function' ? ', with a controller' : ', with no controller');
};

// what uiCustomViewThroughServer runs first, asynchronously - it declares the callbacks: it changes the answer through
// the controller it is given, and says it is done once the server has
window.uiCustomViewThroughServer = function (a, controller, resolve, reject) {
    window.uiCustomViewThroughServerState = { changed: false, callbacks: typeof resolve + ' ' + typeof reject };
    controller.change('uiCustomViewAnswer', 'the server says ' + a * 3).then(function () {
        window.uiCustomViewThroughServerState.changed = true;
        resolve();
    }, reject);
};

// ... and next, in the page, whether the change of the first one was over when it ran, and the callbacks that one was
// given
window.uiCustomViewAfterServer = function () {
    var state = window.uiCustomViewThroughServerState;
    window.uiCustomViewAfterServerSaw = (state.changed ? 'after the change' : 'before the change') + ', given ' + state.callbacks;
};

// the custom cell renderer of uiCustomViewCellText: a field whose change sets the property's value through the
// renderer's controller, and a button that runs an action through the form's controller it reaches from there
window.uiCustomViewCell = function () {
    return {
        render: function (element) {
            var field = document.createElement('input'), exec = document.createElement('button');
            var result = document.createElement('span');
            field.className = 'ui-custom-view-cell-field';
            exec.className = 'ui-custom-view-cell-exec';
            exec.textContent = 'exec';
            result.className = 'ui-custom-view-cell-result';
            element.append(field, exec, result);
        },
        update: function (element, controller, value) {
            var field = element.querySelector('.ui-custom-view-cell-field');
            field.value = value === null ? '' : value;
            field.onchange = function () { controller.change(field.value); };
            element.querySelector('.ui-custom-view-cell-exec').onclick = function () {
                controller.form.exec('uiCustomViewDouble', 4).then(function (doubled) {
                    element.querySelector('.ui-custom-view-cell-result').textContent = 'exec: ' + typeof doubled + ' ' + doubled;
                });
            };
        }
    };
};

// what the navigator's action uiCustomViewFromNavigator runs, asynchronously, with the navigator's controller: the
// action uiCustomViewNavigatorMark run by name with what a script returns for the parameter
window.uiCustomViewFromNavigator = function (a, controller, resolve, reject) {
    controller.eval('run(INTEGER a) { RETURN a * 7; }', a).then(function (value) {
        return controller.exec('UiCustomView.uiCustomViewNavigatorMark', 'navigator ' + typeof value + ' ' + value);
    }).then(resolve, reject);
};
