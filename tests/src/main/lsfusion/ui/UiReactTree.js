// the React views UiReactTreeIT uses for the trees of UiReactTree.lsf, in plain JS. Each shows its data as text, and
// hands on its data and controller (window.uiRtr...State) for the test to call the members with

// the tree of uiRtrDrawn, drawn as a tree is: a line per category - whether it is open, has no children, is loading
// them -, and under it a line per item whose `parent` it is, open or not, in the order of the items' list; the items
// whose parent is no category it has; the tree's current row - none, or a key of no row -, the current objects the
// server reads, and the tree's sortings
window.UiRtrDrawn = function (props) {
    var e = React.createElement, data = props.data;
    window.uiRtrDrawnState = {data: data, controller: props.controller};
    var categoryOf = function (key) {
        var row = key === null ? null : data.c.byKey[key];
        return row ? row.cName.value : 'nothing';
    };
    var current = function () {
        var key = data.t.current, row = key === null ? null : data.c.byKey[key] || data.i.byKey[key];
        return key === null ? 'none' : row ? (row.cName || row.iName).value : 'a key of no row';
    };
    return e('div', null,
        e('div', {className: 'ui-rtr-current'}, current()),
        e('div', {className: 'ui-rtr-server-current'}, 'category ' + (data.c.currentCategory.value || 'none') + ', item '
            + (data.i.currentItem.value || 'none')),
        data.i.list.filter(function (item) { return !(item.parent in data.c.byKey); }).map(function (item) {
            return e('div', {key: item.key, className: 'ui-rtr-orphan'}, item.iName.value + ' ' + item.qty.value);
        }),
        e('div', {className: 'ui-rtr-orders'}, data.t.orders.map(function (order) {
            return order.property + (order.desc ? ' desc' : ' asc');
        }).join(', ')),
        data.c.list.map(function (category) {
            return e('div', {key: category.key},
                e('div', {className: 'ui-rtr-category'}, category.cName.value + (category.expanded ? ' open' : ' closed')
                    + (category.hasChildren ? '' : ' empty') + (category.loading ? ' loading' : '')
                    + (category.parent === null ? '' : ' with a parent')),
                data.i.list.filter(function (item) { return item.parent === category.key; }).map(function (item) {
                    return e('div', {key: item.key, className: 'ui-rtr-item'},
                        item.iName.value + ' ' + item.qty.value + ' in ' + categoryOf(item.parent));
                }));
        }));
};

// the recursive tree of uiRtrFolders: a line per folder, saying the folder it hangs in and whether it is open
window.UiRtrFolders = function (props) {
    var e = React.createElement, data = props.data;
    window.uiRtrFoldersState = {data: data, controller: props.controller};
    return e('div', null, data.f.list.map(function (row) {
        var parent = row.parent === null ? null : data.f.byKey[row.parent];
        return e('div', {key: row.key, className: 'ui-rtr-folder'}, row.fName.value
            + (row.parent === null ? ' at the top' : ' in ' + (parent ? parent.fName.value : 'nothing'))
            + (row.expanded ? ' open' : row.hasChildren ? ' closed' : ' empty'));
    }));
};

// the user filters of uiRtrFiltered's tree, the conditions of all its groups, as text: each one's property, comparison
// and value with the value's type, whether it is negated and joined to the next one by OR
window.UiRtrFilters = function (props) {
    var e = React.createElement, data = props.data;
    window.uiRtrFiltersState = {data: data, controller: props.controller};
    return e('div', {className: 'ui-rtr-filters'}, data.t.filters.map(function (condition) {
        return condition.property + ' ' + condition.compare + ' ' + condition.value + ' (' + typeof condition.value + ')'
            + (condition.negation ? ' not' : '') + (condition.or ? ' or' : '');
    }).join('; ') || 'no conditions');
};
