// the React views UiReactPlacingIT uses for the forms of UiReactPlacing.lsf. The platform's components and hooks are
// read inside each view, as the runtime is installed only when the first view mounts. Every clickable has a text of its
// own

// the board of uiRplBoard: the note, the action and the memo the platform draws, placed by their names - the note in a
// host of a fixed height -, the items' quantities placed per row. Its buttons switch the hosts to the mistakes a
// placement can make and back, and change the class the view gives its hosts
window.UiRplBoard = function (props) {
    var e = React.createElement, lsfusion = window.lsfusion, Platform = lsfusion.Platform;
    var data = props.data, rows = data.i.list;
    var mode = React.useState(false), mistakes = mode[0], setMistakes = mode[1];
    var classes = React.useState('ui-rpl-a'), cls = classes[0], setCls = classes[1];
    // a host of an element the view draws anyway, with a child of the view's own that carries lsf-view-error too
    var own = lsfusion.usePlatform(mistakes ? 'uiRplNothing' : 'uiRplMemo', {className: 'ui-rpl-own-host ' + cls});
    var children = [
        e('button', {key: 'mistakes', onClick: function () { setMistakes(true); }}, 'mistakes'),
        e('button', {key: 'right', onClick: function () { setMistakes(false); }}, 'right'),
        e('button', {key: 'class', onClick: function () { setCls(cls === 'ui-rpl-a' ? 'ui-rpl-b' : 'ui-rpl-a'); }}, 'switch class'),
        e('span', {key: 'code', className: 'ui-rpl-code'}, String(data.c.cCode.value)),
        // taller than the platform's view of the note, which fills it
        e(Platform, {key: 'note', name: mistakes ? 'c.nope' : 'c.note', className: 'ui-rpl-note ' + cls, style: {height: '120px'}}),
        e(Platform, {key: 'toggle', name: 'uiRplToggleNote', className: 'ui-rpl-toggle'}),
        e('div', Object.assign({key: 'own'}, own), e('b', {className: 'ui-rpl-own lsf-view-error'}, 'mine')),
        e('div', {key: 'rows'}, rows.map(function (row) {
            return e('div', {key: row.key, className: 'ui-rpl-row'},
                e('span', {className: 'ui-rpl-name'}, row.iName.value),
                e(Platform, {name: 'i.qty', row: row, className: 'ui-rpl-qty'}));
        }))
    ];
    if (mistakes)
        children.push(
            // drawn per row, given no row
            e(Platform, {key: 'no-row', name: 'i.qty', className: 'ui-rpl-no-row'}),
            // a row for what has one view
            e(Platform, {key: 'one-view', name: 'c.note', row: rows[0], className: 'ui-rpl-one-view'}),
            // one the view draws itself
            e(Platform, {key: 'drawn', name: 'c.cCode', className: 'ui-rpl-drawn'}),
            // a column the view draws
            e(Platform, {key: 'column', name: 'i.iName', className: 'ui-rpl-column'}),
            // a row that is no row
            e(Platform, {key: 'no-row-at-all', name: 'i.qty', row: {key: 'none'}, className: 'ui-rpl-not-a-row'}));
    return e('div', {className: 'ui-rpl-board'}, children);
};

// the view of uiRplLabels: places one property the platform draws and draws only the caption of another, as a tab
// strip draws the title of a page it does not show
window.UiRplLabels = function (props) {
    var e = React.createElement;
    return e('div', null,
        e('span', {className: 'ui-rpl-unplaced-caption'}, props.data.uiRplUnplaced.caption),
        e(window.lsfusion.Platform, {name: 'uiRplOther', className: 'ui-rpl-other'}));
};

// the view of uiRplShows: places a container (through its own element), a property and an action the platform draws,
// and says what their entries say; its buttons show and hide the container and the property through their actions
window.UiRplShows = function (props) {
    var e = React.createElement, lsfusion = window.lsfusion, data = props.data, controller = props.controller;
    var box = lsfusion.usePlatform('uiRplBox', {className: 'ui-rpl-box'});
    return e('div', null,
        e('section', box),
        e(lsfusion.Platform, {name: 'uiRplShowsNote', className: 'ui-rpl-shown-note'}),
        e(lsfusion.Platform, {name: 'uiRplGo', className: 'ui-rpl-go'}),
        e('span', {className: 'ui-rpl-box-entry'}, data.uiRplBox.hidden ? 'box hidden' : 'box shown'),
        e('span', {className: 'ui-rpl-note-entry'}, data.uiRplShowsNote.hidden ? 'note hidden' : 'note shown'),
        e('span', {className: 'ui-rpl-note-comment'}, data.uiRplShowsNote.comment),
        e('span', {className: 'ui-rpl-go-entry'}, Object.keys(data.uiRplGo).sort().join() || 'nothing'),
        e('span', {className: 'ui-rpl-gone'}, String(data.uiRplGone.value)),
        e('button', {onClick: function () { controller.uiRplToggleBox.exec(); }}, 'toggle box'),
        e('button', {onClick: function () { controller.uiRplToggleShowsNote.exec(); }}, 'toggle note'));
};

// the view of uiRplFloating: content of a fixed size, so that a window measured before it is drawn does not hold it
window.UiRplFloating = function (props) {
    return React.createElement('div', {className: 'ui-rpl-floating', style: {width: '400px', height: '300px'}},
        props.data.uiRplFloatText.value);
};

// the view of uiRplShowIf: says what the entries of the container it draws, of the property's filter in it and of the
// property in it say
window.UiRplShowIf = function (props) {
    var e = React.createElement, data = props.data;
    return e('div', null,
        e('span', {className: 'ui-rpl-container-entry'}, data.uiRplShownContainer.hidden ? 'container hidden' : 'container shown'),
        e('span', {className: 'ui-rpl-filter-entry'}, data['FILTER(uiRplShowIfName(i))'].hidden ? 'filter hidden' : 'filter shown'),
        e('span', {className: 'ui-rpl-label-entry'}, data.uiRplLabel.hidden ? 'label hidden' : 'label ' + data.uiRplLabel.value));
};

// the view of uiRplTabbed: draws both pages of the `tabbed` container side by side; it hands on its controller
// (window.uiRplTabbedController) for the test to call its editing verbs with
window.UiRplTabbed = function (props) {
    var e = React.createElement, data = props.data;
    window.uiRplTabbedController = props.controller;
    return e('div', {className: 'ui-rpl-tabbed'},
        e('span', {className: 'ui-rpl-first-page'}, data.uiRplFirstPage.value),
        e('span', {className: 'ui-rpl-second-page'}, data.uiRplSecondPage.value));
};
