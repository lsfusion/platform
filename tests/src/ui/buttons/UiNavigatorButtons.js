// every window it draws places its platform-drawn entries depth-first in one flat row
window.UiNavigatorButtons = function (props) {
    var e = React.createElement, Platform = window.lsfusion.Platform, data = props.data, places = [];
    function visit(name) {
        var entry = data.byName[name];
        if (entry.platformDrawn)
            places.push(e(Platform, { key: name, name: name, className: 'ui-nb-place' }));
        entry.children.forEach(visit);
    }
    data.root.forEach(visit);
    // and a menu's own .navbar-nav, aligned its own way, which the vertical navbar's strip would align to the start
    places.push(e('div', { key: 'own', className: 'navbar-nav ui-nb-own', style: { alignItems: 'center' } }));
    return e('div', { className: 'ui-nb-row',
        style: { display: 'flex', alignItems: 'center', gap: '8px', height: '100%' } }, places);
};
