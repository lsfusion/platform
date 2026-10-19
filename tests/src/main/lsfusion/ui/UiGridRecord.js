// UiGridRecordIT's views of UiGridRecord.lsf: a classic custom view of cards that pops the grid's record up next to the
// card it clicks (uiGridRecordCards), and a React view around a grid (UiGridRecordBoard) that places a card of the
// record the platform draws, draws another container of it, which it shows or not with useShown, and places a nested
// React view of it (UiGridRecordNested). It writes out what its entries say - the captions, whether the placed card is
// hidden, the code the drawn container holds, the bumps counted outside it -, and its buttons place, show and bump
function uiGridRecordCards() {
    return {
        render: function (element) {
            // each card as wide as its name: the record popped up next to one leaves the others free to click
            element.cards = document.createElement('div');
            element.cards.style.cssText = 'display: flex; flex-direction: column; align-items: flex-start';
            element.appendChild(element.cards);
        },
        update: function (element, controller, list) {
            var cards = element.cards;
            while (cards.lastElementChild)
                cards.removeChild(cards.lastElementChild);
            list.forEach(function (item) {
                var card = document.createElement('div');
                card.className = 'ui-grid-record-card';
                card.textContent = item.uiGridRecordName;
                card.onclick = function () { controller.changeObject(item, true, card); };
                cards.appendChild(card);
            });
        }
    };
}

window.UiGridRecordBoard = function (props) {
    var e = React.createElement, lsfusion = window.lsfusion, data = props.data, controller = props.controller;
    var state = React.useState({placed: true, drawn: true}), shown = state[0], setShown = state[1];
    lsfusion.useShown('recordDrawn', shown.drawn);
    var button = function (text, onClick) {
        return e('button', {onClick: onClick}, text);
    };
    return e('div', null,
        data.j.list.map(function (row) {
            return e('div', {key: row.key, className: 'ui-grid-record-row' + (row.isCurrent ? ' ui-grid-record-current' : ''),
                onClick: function () { controller.j.change(row); }}, row.uiGridRecordName.value);
        }),
        e('div', {className: 'ui-grid-record-placed-caption'}, data.recordPlaced.caption),
        e('div', {className: 'ui-grid-record-placed-hidden'}, String(!!data.recordPlaced.hidden)),
        e('div', {className: 'ui-grid-record-drawn-caption'}, data.recordDrawn.caption),
        e('div', {className: 'ui-grid-record-code'}, String(data.j.drawnCode.value)),
        e('div', {className: 'ui-grid-record-bumps'}, String(data.uiGridRecordBumps.value)),
        button('Unplace', function () { setShown({placed: false, drawn: shown.drawn}); }),
        button('Place', function () { setShown({placed: true, drawn: shown.drawn}); }),
        button('Hide drawn', function () { setShown({placed: shown.placed, drawn: false}); }),
        button('Show drawn', function () { setShown({placed: shown.placed, drawn: true}); }),
        button('Bump', function () { controller.j.uiGridRecordBump.exec(); }),
        shown.placed ? e(lsfusion.Platform, {name: 'recordPlaced'}) : null,
        e(lsfusion.Platform, {name: 'recordNested'}));
};

window.UiGridRecordNested = function () {
    return React.createElement('div', {className: 'ui-grid-record-nested'}, 'nested');
};
