// UiFormsWindowIT's React views: the component that draws the window uiFormsWindowBoard, and the view of the form
// uiFormsWindowPush

// the window uiFormsWindowBoard: while a form is open, a strip with a tab per open form - the caption, which selects
// the form, and a button that closes it -, a button that closes them all, a button per edit mode, which chooses it, the
// mode chosen, a button that turns full screen on or off, and the places of the forms. The component keeps what it is
// given and its controller in window.uiFormsWindowBoard. What it places is the test's to switch with its place():
// every open form ('all'), the current one ('current'), the current one in two places ('twice'), the current one in
// the second of those alone ('second'), or a name no open form has ('none'). With nothing open it draws nothing, so
// the empty window takes no room
window.UiFormsWindowBoard = function (props) {
    var e = React.createElement, data = props.data, controller = props.controller;
    var placing = React.useState('all'), place = placing[0];
    window.uiFormsWindowBoard = { data: data, controller: controller, place: placing[1] };
    if (data.open.length === 0)
        return null;

    var current = data.open.filter(function (name) { return data.byName[name].selected; })[0];
    function host(key, name) {
        return e(window.lsfusion.Platform, { key: key, name: name, className: 'ui-forms-window-board-' + key });
    }
    var hosts = place === 'all' ? data.open.map(function (name) { return host('placed-' + name, name); })
        : !current ? []
        : place === 'current' ? [host('current', current)]
        : place === 'twice' ? [host('first', current), host('second', current)]
        : place === 'second' ? [host('second', current)]
        : [host('none', 'form-none')];

    return e('div', { className: 'ui-forms-window-board-view' },
        e('div', { className: 'ui-forms-window-board-strip' },
            data.open.map(function (name) {
                var entry = data.byName[name];
                return e('span', { key: name, 'data-form': entry.canonicalName,
                                   className: 'ui-forms-window-board-tab' + (entry.selected ? ' ui-forms-window-board-selected' : '')
                                              + (entry.blocked ? ' ui-forms-window-board-blocked' : '') },
                    e('button', { className: 'ui-forms-window-board-select', onClick: function () { controller.select(name); } }, entry.caption),
                    e('button', { className: 'ui-forms-window-board-close', onClick: function () { controller.close(name); } }, 'x'));
            }),
            e('button', { className: 'ui-forms-window-board-close-all', onClick: function () { controller.closeAll(); } }, 'Close all')),
        e('div', { className: 'ui-forms-window-board-modes' },
            data.editModes.map(function (mode) {
                return e('button', { key: mode.name, 'data-mode': mode.name, className: 'ui-forms-window-board-mode',
                                     onClick: function () { controller.setEditMode(mode.name); } }, mode.caption);
            }),
            e('span', { className: 'ui-forms-window-board-edit-mode' }, data.editMode),
            e('button', { className: 'ui-forms-window-board-full-screen',
                          onClick: function () { controller.toggleFullScreen(); } }, 'Full screen')),
        hosts);
};

// the form uiFormsWindowPush: buttons that change its four properties the way a view does, through their members, and
// three that run actions by their names - the open of First, the close of the form followed by an open of First, and
// the open of the close source
window.UiFormsWindowPush = function (props) {
    var e = React.createElement, controller = props.controller;
    return e('div', { className: 'ui-forms-window-push' },
        e('button', { className: 'ui-forms-window-push-stored', onClick: function () { controller.uiFormsWindowStored.change('changed'); } }, 'Change stored'),
        e('button', { className: 'ui-forms-window-push-closer', onClick: function () { controller.uiFormsWindowCloser.change(1); } }, 'Change closer'),
        e('button', { className: 'ui-forms-window-push-plain', onClick: function () { controller.uiFormsWindowPlain.change(7); } }, 'Change plain'),
        e('button', { className: 'ui-forms-window-push-opener', onClick: function () { controller.uiFormsWindowOpener.change(1); } }, 'Change opener'),
        e('button', { className: 'ui-forms-window-push-by-name', onClick: function () { controller.exec('uiFormsWindowFirstOnce'); } }, 'Open by name'),
        e('button', { className: 'ui-forms-window-push-close-and-show',
                      onClick: function () { controller.exec('uiFormsWindowCloseAndShowFirst'); } }, 'Close and show by name'),
        e('button', { className: 'ui-forms-window-push-close-source',
                      onClick: function () { controller.exec('uiFormsWindowCloseSourceIntoForms'); } }, 'Close source by name'));
};
