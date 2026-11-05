// UiDesktopIT's layout of the navigator's windows (NAVIGATOR CUSTOM 'UiDesktop'): the top strip, the toolbar beside the
// forms, the log under them, and what the test asks it to show besides with window.uiDesktop.show - the aside window
// ('aside') and a place for a name no window has ('wrong'). It keeps what it is given in window.uiDesktop, and says on
// its root whether the platform hides the toolbar
window.UiDesktop = function (props) {
    var e = React.createElement, Platform = window.lsfusion.Platform, data = props.data;
    var showing = React.useState({}), shown = showing[0];
    window.uiDesktop = {
        data: data,
        show: function (key) {
            showing[1](function (before) {
                var after = Object.assign({}, before);
                after[key] = true;
                return after;
            });
        }
    };

    function place(name, className, style) {
        return e(Platform, { name: name, className: className, style: style });
    }
    return e('div', { className: 'ui-desktop', 'data-toolbar-hidden': String(data['System.toolbar'].hidden),
                      style: { display: 'flex', flexDirection: 'column', height: '100%' } },
        e('div', { className: 'ui-desktop-top', style: { display: 'flex', height: '48px' } },
            place('System.logo', 'ui-desktop-logo'),
            place('System.root', 'ui-desktop-root', { flex: 1 }),
            place('System.system', 'ui-desktop-system')),
        e('div', { className: 'ui-desktop-body', style: { display: 'flex', flex: 1, minHeight: 0 } },
            place('System.toolbar', 'ui-desktop-toolbar', { width: '200px' }),
            place('System.forms', 'ui-desktop-forms', { flex: 1 }),
            shown.aside && place('UiDesktop.uiDesktopAside', 'ui-desktop-aside', { width: '300px' }),
            shown.wrong && place('UiDesktop.nope', 'ui-desktop-wrong', { width: '100px' })),
        place('System.log', 'ui-desktop-log', { height: '80px' }));
};
