// UiReactProjectionIT's views of the forms of UiReactProjection.lsf: React views in plain JS, reading the platform's
// hooks inside the view, and two classic custom views of a group. Every clickable has a text of its own

// the view of uiRpjMembers: the rows with their names and prices - or that a cell has no value -, the entries of the
// price column, of the label and of the grid, the row that is current and the name the server reads for the current
// object, and whether the price's member is the one the view got first; its buttons change the first row's price
// through the member, to 42 or 44 - the refusal of the call shown -, and through the one the view got first, and make a
// row current through the group's member, by the row and by its key
window.UiRpjMembers = function (props) {
    var e = React.createElement, data = props.data, controller = props.controller, rows = data.i.list;
    var refusal = React.useState(''), refused = refusal[0], setRefused = refusal[1];
    if (!window.uiRpjMember)
        window.uiRpjMember = controller.i.price; // the member as the view first got it
    var label = data.uiRpjLabel, current = rows.filter(function (row) { return row.isCurrent; })[0];
    var value = function (entry) { return 'value' in entry ? String(entry.value) : 'no value'; };
    var change = function (price) {
        try {
            controller.i.price.change(price, rows[0]);
            setRefused('');
        } catch (error) {
            setRefused(error.message);
        }
    };
    return e('div', null,
        e('span', {className: 'ui-rpj-price-entry'}, (data.i.price.hidden ? 'price hidden' : 'price shown') + ' ' + data.i.price.caption),
        e('span', {className: 'ui-rpj-same-member'}, window.uiRpjMember === controller.i.price ? 'the same member' : 'another member'),
        e('span', {className: 'ui-rpj-label'}, (label.hidden ? 'hidden ' : 'shown ') + value(label)),
        e('span', {className: 'ui-rpj-grid'}, data['GRID(i)'].hidden ? 'grid hidden' : 'grid shown'),
        e('span', {className: 'ui-rpj-current'}, current ? current.name.value : 'none'),
        e('span', {className: 'ui-rpj-current-name'}, value(data.i.currentName)),
        e('span', {className: 'ui-rpj-refusal'}, refused),
        rows.map(function (row) {
            return e('div', {key: row.key, className: 'ui-rpj-row'}, value(row.name) + '|' + value(row.price));
        }),
        e('button', {onClick: function () { change(42); }}, 'price 42'),
        e('button', {onClick: function () { change(44); }}, 'price 44'),
        e('button', {onClick: function () { window.uiRpjMember.change(43, rows[0]); }}, 'price 43 by the first member'),
        e('button', {onClick: function () { controller.i.change(rows[1]); }}, 'second current'),
        e('button', {onClick: function () { controller.i.change(data.i.keys[0]); }}, 'first current by key'));
};

// the view of uiRpjValues: the rows' colors and periods; its button gives the second row the first one's, as the view
// was given them
window.UiRpjValues = function (props) {
    var e = React.createElement, controller = props.controller, rows = props.data.i.list;
    return e('div', null,
        rows.map(function (row) {
            return e('div', {key: row.key, className: 'ui-rpj-value-row'}, row.name.value + '|' + row.color.value + '|' + row.period.value);
        }),
        e('button', {onClick: function () {
            controller.i.color.change(rows[0].color.value, rows[1]);
            controller.i.period.change(rows[0].period.value, rows[1]);
        }}, 'copy to the second'));
};

// the view of uiRpjLoading: the rows' quantities and the action, each saying whether it waits for an answer; its
// buttons run the action and change the first row's quantity through their members
window.UiRpjLoading = function (props) {
    var e = React.createElement, data = props.data, controller = props.controller, rows = data.i.list;
    return e('div', null,
        e('span', {className: 'ui-rpj-run'}, data.uiRpjRun.loading ? 'run loading' : 'run answered'),
        e('span', {className: 'ui-rpj-counter'}, String(data.uiRpjCounter.value)),
        rows.map(function (row) {
            return e('div', {key: row.key, className: 'ui-rpj-qty'},
                row.name.value + '|' + row.qty.value + (row.qty.loading ? '|loading' : ''));
        }),
        e('button', {onClick: function () { controller.uiRpjRun.exec(); }}, 'run'),
        e('button', {onClick: function () { controller.i.qty.change(7, rows[0]); }}, 'qty 7'));
};

// the classic custom view of uiRpjLoading's second group: there only to hand its controller on
window.uiRpjClassic = function () {
    return {
        render: function (element, controller) {
            window.uiRpjClassicController = controller;
            element.textContent = 'classic';
        },
        update: function (element, controller) {
            window.uiRpjClassicController = controller;
        }
    };
};

// a tab of uiRpjPages' strip: mounted for as long as the strip is, whichever page is shown, saying whether its page is
function UiRpjTab(props) {
    window.lsfusion.useShown(props.name, props.active);
    return React.createElement('button', {onClick: props.onClick}, props.caption + (props.active ? ' shown' : ''));
}

// the view of uiRpjPages: a strip of two pages it shows one of; the value it is given of the property on the second
// page and of the one beside the pages
window.UiRpjPages = function (props) {
    var e = React.createElement, data = props.data;
    var shown = React.useState('A'), active = shown[0], setActive = shown[1];
    return e('div', null,
        e(UiRpjTab, {name: 'uiRpjPageA', caption: 'page A', active: active === 'A', onClick: function () { setActive('A'); }}),
        e(UiRpjTab, {name: 'uiRpjPageB', caption: 'page B', active: active === 'B', onClick: function () { setActive('B'); }}),
        e('span', {className: 'ui-rpj-witness'}, String(data.uiRpjWitness.value)),
        e('span', {className: 'ui-rpj-count'}, String(data.uiRpjCount.value)));
};

// the view of uiRpjColors: each row's own background; a row's button runs the action of the row that flags it
window.UiRpjColors = function (props) {
    var e = React.createElement, controller = props.controller;
    return e('div', null, props.data.i.list.map(function (row) {
        return e('div', {key: row.key, className: 'ui-rpj-color-row'},
            e('span', null, row.name.value + '|' + row.background),
            e('button', {onClick: function () { controller.i.toggle.exec(row); }}, 'toggle ' + row.name.value));
    }));
};

// the classic custom view of uiRpjClassic's items: their names and prices; it hands on its controller and its rows, as
// window.uiRpjCardsState
window.uiRpjCards = function () {
    return {
        render: function (element) {
        },
        update: function (element, controller, list) {
            window.uiRpjCardsState = {controller: controller, list: list};
            element.textContent = '';
            list.forEach(function (row) {
                var card = document.createElement('div');
                card.className = 'ui-rpj-card';
                card.textContent = row.name + '|' + row.price;
                element.appendChild(card);
            });
        }
    };
};
