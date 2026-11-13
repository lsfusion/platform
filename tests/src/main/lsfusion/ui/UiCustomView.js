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

// buttons that call the server through the form's controller, each listing what its call resolved with - the type and
// the value - or was rejected with; the controller is kept as window.uiCustomViewCalls, to be called once the form is
// closed. As the view is unmounted - the form taken out of the page - it runs what a test left in
// window.uiCustomViewUnmounted, given the controller: in a layout effect's cleanup, which React runs at once, while the
// platform is taking the form out
window.UiCustomViewCalls = function (props) {
    var e = React.createElement, controller = props.controller;
    var said = React.useState([]), results = said[0], setResults = said[1];
    window.uiCustomViewCalls = controller;
    React.useLayoutEffect(function () {
        return function () {
            var unmounted = window.uiCustomViewUnmounted;
            window.uiCustomViewUnmounted = undefined;
            if (unmounted)
                unmounted(controller);
        };
    }, []);
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
        button('eval', function () { return controller.form.eval('run(INTEGER a) { RETURN a * 2; }', 21); }),
        button('evalAction', function () { return controller.form.evalAction('UiCustomView.uiCustomViewCount() <- $1;', 5); }),
        button('read', function () { return controller.form.eval('run() { RETURN UiCustomView.uiCustomViewCount(); }'); }),
        button('exec', function () { return controller.form.exec('uiCustomViewDouble', 4); }),
        button('change', function () { return controller.form.change('uiCustomViewCount', 9); }),
        button('broken', function () { return controller.form.eval('run() { RETURN 1 +; }'); }),
        button('missing', function () { return controller.form.exec('uiCustomViewNoSuchAction'); }),
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
// renderer's controller, and a button that runs an action through the form's controller it reaches from there. Named
// as a component is and not registered as one, it stays a classic render function
window.UiCustomViewCell = function () {
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

// a context of the application's own, which a view provides to what it draws: what a component the platform draws
// reads of it says whose tree it is drawn in - 'none' in a root of its own
window.UiCustomViewTheme = React.createContext('none');

// the React component a property's CUSTOM names, registered as one: a button that shows the cell's value and adds 10
// to it through the controller it was first given. It tells its instances apart, and says whether that controller is
// still the one it is given and whether its props are what the hooks give, and the theme around it - and hands its
// controller over as it is unmounted. While the test says so (window.uiCustomViewStops, the event's type) it stops
// a mouse press or a key press in it, counting them in window.uiCustomViewStopped
lsfusion.custom.register('UiCustomViewReactCell', function ({data, controller}) {
    const {createElement: h, useEffect, useState} = React;
    const theme = React.useContext(window.UiCustomViewTheme);
    const stop = event => {
        if (window.uiCustomViewStops === event.type) {
            window.uiCustomViewStopped = (window.uiCustomViewStopped || 0) + 1;
            event.stopPropagation();
        }
    };
    const [instance] = useState(() => window.uiCustomViewCellInstances = (window.uiCustomViewCellInstances || 0) + 1);
    const [initialController] = useState(() => controller);
    const currentController = lsfusion.useController();
    const currentData = lsfusion.useData();
    useEffect(function () {
        return function () { window.uiCustomViewReleasedCell = controller; };
    }, [controller]);
    return h('button', {
        ref: button => { if (button) button.testController = controller; },
        className: 'ui-custom-react-cell',
        'data-instance': instance,
        'data-stable-controller': String(controller === initialController),
        'data-matching-context': String(controller === currentController && data === currentData),
        'data-theme': theme,
        onMouseDown: stop,
        onKeyDown: stop,
        disabled: data.readOnly || data.disabled,
        onClick: () => initialController.change((data.value || 0) + 10)
    }, String(data.value));
}, 'reactView');

// the React component a group's CUSTOM names: a card for each row of the group's node it is given, drawn by the
// platform's List with no group named, which makes the row current and adds 10 to its quantity through the group's
// member, under the title the group's OPTIONS give
function UiCustomViewCard({row, controller, caption}) {
    const h = React.createElement;
    return h('div', {
        className: 'ui-custom-card',
        'data-current': String(row.current),
        'data-name-read-only': String(row.uiCustomViewName.readOnly === true)
    },
        h('button', {onClick: () => controller.change(row)}, row.uiCustomViewName.value),
        h('button', {className: 'ui-custom-card-quantity', title: caption,
            onClick: () => controller.uiCustomViewQuantity.change(row.uiCustomViewQuantity.value + 10, row.key)
        }, row.uiCustomViewQuantity.value));
}
lsfusion.custom.register('UiCustomViewCards', function ({data, controller}) {
    const h = React.createElement;
    return h('div', {className: 'ui-custom-cards', 'data-properties': data.properties.join(','), 'data-theme': React.useContext(window.UiCustomViewTheme),
            'data-comment': data.uiCustomViewQuantity.comment || 'none',
            'data-default': data.uiCustomViewQuantity.defaultValue || 'none', 'data-view-filters': String(data.viewFilters.length)},
        data.options && data.options.title,
        // the view's own filter of its rows: the quantities over 1
        h('button', {className: 'ui-custom-cards-filter',
            onClick: () => controller.viewFilters.change([{property: 'uiCustomViewQuantity', compare: '>', value: 1}])}, 'Over 1'),
        h(lsfusion.List, {component: UiCustomViewCard, controller: controller, caption: data.uiCustomViewQuantity.caption}));
}, 'reactView');

// the container of uiCustomViewPlacedGroup: it places the grid the platform draws in its CUSTOM view
window.UiCustomViewCardsOuter = function () {
    return React.createElement('div', {className: 'ui-custom-cards-outer'}, React.createElement(lsfusion.Platform, {name: 'GRID(i)'}));
};

// the React view of the grid of uiCustomViewPlacedProperties: it draws the rows, and in each places the quantity the
// platform draws with a React component and the one it draws with its own editor
lsfusion.custom.register('UiCustomViewPlacedGrid', function ({data, controller}) {
    const h = React.createElement;
    return h('div', {className: 'ui-custom-placed-grid'},
        data.list.map(row => h('div', {key: row.key, className: 'ui-custom-placed-row', 'data-current': String(row.current)},
            h('button', {onClick: () => controller.change(row)}, row.uiCustomViewName.value),
            h(lsfusion.Platform, {name: 'uiCustomViewQuantity', row: row}),
            h(lsfusion.Platform, {name: 'standardQuantity', row: row, className: 'ui-custom-standard-cell'}))));
}, 'reactView');

// ... and the container around it: the grid, and the quantity of the current item, under its theme
window.UiCustomViewPlacedOuter = function () {
    const h = React.createElement;
    return h(window.UiCustomViewTheme.Provider, {value: 'placed'},
        h(lsfusion.Platform, {name: 'GRID(i)'}),
        h(lsfusion.Platform, {name: 'i.panelQuantity', className: 'ui-custom-placed-panel'}));
};

// the container of uiCustomViewEmbedded: under its theme, the table the platform draws, whose quantities a React
// component draws, the group the platform draws with a React component, and - while the test says so,
// window.uiCustomViewEmbedded.placeInner - the container below with a component of its own, in a place given ownRoot
// if the test says so (ownRootInner). It counts the key presses that reach it in window.uiCustomViewOuterKeys
window.UiCustomViewEmbeddedOuter = function () {
    const h = React.createElement, Platform = lsfusion.Platform;
    const [inner, placeInner] = React.useState(true);
    const [ownRoot, ownRootInner] = React.useState(false);
    window.uiCustomViewEmbedded = {placeInner: placeInner, ownRootInner: ownRootInner};
    return h(window.UiCustomViewTheme.Provider, {value: 'outer'},
        h('div', {className: 'ui-custom-embedded',
                  onKeyDown: () => { window.uiCustomViewOuterKeys = (window.uiCustomViewOuterKeys || 0) + 1; }},
            h(Platform, {name: 'GRID(i)', className: 'ui-custom-embedded-table'}),
            h(Platform, {name: 'GRID(c)', className: 'ui-custom-embedded-cards'}),
            inner && h(Platform, {name: 'inner', className: 'ui-custom-embedded-inner', ownRoot: ownRoot})));
};

// ... and the component of the container inside it: the theme around it and which instance it is, and, under a
// theme of its own, the count the platform draws
window.UiCustomViewEmbeddedInner = function () {
    const h = React.createElement;
    const [instance] = React.useState(() => window.uiCustomViewInnerInstances = (window.uiCustomViewInnerInstances || 0) + 1);
    return h(window.UiCustomViewTheme.Provider, {value: 'inner'},
        h('span', {className: 'ui-custom-embedded-inner-view', 'data-instance': instance,
                   'data-theme': React.useContext(window.UiCustomViewTheme)}, 'inner'),
        h(lsfusion.Platform, {name: 'uiCustomViewCount', className: 'ui-custom-embedded-count'}));
};

// the classic render function a group's CUSTOM names, named as a component is and not registered as one
window.UiCustomViewClassicGroup = function () {
    return {
        render: function () {},
        update: function (element, controller, list) {
            element.textContent = list.length + ' classic rows';
            element.setAttribute('data-marked', String(list.length > 0 && 'mark' in list[0]));
            element.setAttribute('data-name-background',
                list.length > 0 && controller.getBackground('uiCustomViewName', list[0]) || 'none');
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
