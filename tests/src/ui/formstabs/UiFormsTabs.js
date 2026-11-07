// the component's own tabs deliberately reuse the names the standard tabs' CSS rules apply to
window.UiFormsTabs = function (props) {
    var e = React.createElement, Platform = window.lsfusion.Platform, Caption = window.lsfusion.Caption;
    var data = props.data, controller = props.controller;
    var current = data.open.find(function (name) { return data.byName[name].selected; });
    return e('div', { className: 'ui-forms-tabs', style: { display: 'flex', flexDirection: 'column', height: '100%' } },
        e('div', { className: 'ui-forms-tabs-bar', style: { display: 'flex' } }, data.open.map(function (name) {
            var entry = data.byName[name];
            return e('span', { key: name, className: 'nav-link' + (entry.selected ? ' active' : ''),
                onClick: function () { controller.select(name); } },
                e(Caption, { value: entry.caption }),
                e('button', { type: 'button', className: 'tab-close-button', disabled: entry.blocked,
                    onClick: function (event) { event.stopPropagation(); controller.close(name); } }, 'Close'));
        })),
        current && e(Platform, { key: current, name: current, className: 'ui-forms-tabs-current',
            style: { flex: '1', minHeight: 0 } }));
};
