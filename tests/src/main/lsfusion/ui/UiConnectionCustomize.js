// what UiConnectionCustomizeIT's codes name, as a code of a connection names only what the page already has

// a window drawn by its entries' captions, the ones at the top and their children
window.UiCcDrawn = function (props) {
    var e = React.createElement, data = props.data, entries = [];
    function visit(name) {
        entries.push(e('span', { key: name, className: 'ui-cc-entry' }, data.byName[name].caption));
        data.byName[name].children.forEach(visit);
    }
    data.root.forEach(visit);
    return e('div', { className: 'ui-cc-drawn', style: { display: 'flex', gap: '8px', minHeight: '24px' } }, entries);
};

// the log window, drawn as an empty mark
window.UiCcLog = function () {
    return React.createElement('div', { className: 'ui-cc-log' });
};

// the windows laid out in a column, in the order they are declared, the forms window taking what is left
window.UiCcLayout = function (props) {
    var e = React.createElement, Platform = window.lsfusion.Platform;
    return e('div', { className: 'ui-cc-layout', style: { display: 'flex', flexDirection: 'column', height: '100%' } },
        props.data.windows.map(function (name) {
            return e(Platform, { key: name, name: name, style: name === 'System.forms' ? { flex: 1, minHeight: 0 } : { minHeight: '32px' } });
        }));
};

// what the code schedules calls, counting the calls
window.uiCcTick = function () {
    window.uiCcTicks = (window.uiCcTicks || 0) + 1;
};
