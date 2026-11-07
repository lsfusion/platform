// both windows place their platform-drawn entries depth-first in one flat row
window.UiNavigatorButtons = function (props) {
    var e = React.createElement, Platform = window.lsfusion.Platform, data = props.data, places = [];
    function visit(name) {
        var entry = data.byName[name];
        if (entry.platformDrawn)
            places.push(e(Platform, { key: name, name: name, className: 'ui-nb-place' }));
        entry.children.forEach(visit);
    }
    data.root.forEach(visit);
    return e('div', { className: 'ui-nb-row',
        style: { display: 'flex', alignItems: 'center', gap: '8px', height: '100%' } }, places);
};
