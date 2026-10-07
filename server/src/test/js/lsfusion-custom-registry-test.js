// What the platform's own custom-view JS does, run in node against the real files: the registry as it is shipped,
// the real containsHtmlTag out of the client's utils.js, and a React double that records what was asked of it.
//
//   node server/src/test/js/lsfusion-custom-registry-test.js
//
// No browser, no build step and no dependencies - the same way the JS it tests is shipped. It is not wired into
// maven: it needs node, which the build does not require.
const fs = require('fs');
const vm = require('vm');
const path = require('path');

const REPO = path.resolve(__dirname, '../../../..'); // server/src/test/js -> the repo root
const registrySrc = fs.readFileSync(REPO + '/server/src/main/resources/web/lsfusion-custom-registry.js', 'utf8');
const utilsSrc = fs.readFileSync(REPO + '/web-client/src/main/webapp/static/js/utils.js', 'utf8');

// the platform's own html-or-text predicate, taken from utils.js as it is written there
const containsHtmlTag = utilsSrc.match(/function containsHtmlTag\(value\) \{[\s\S]*?\n\}/)[0];

let failures = 0, passes = 0;
function check(what, cond, detail) {
    if (cond) { passes++; return; }
    failures++;
    console.log('  FAIL  ' + what + (detail !== undefined ? '  [' + detail + ']' : ''));
}

// ---- the React double -------------------------------------------------------------------------------------------
// A hook called inside render(instance, ...) keeps its state in that instance from one call to the next, the way a
// mounted component keeps it from one render to the next; called outside, it starts afresh every time. An effect is
// queued, as React runs it after the commit, and runs on flushEffects() - and, with deps, only when they changed
function makeReact() {
    // an ES5 function, exactly as React defines it - the registry's boundary does React.Component.call(this, props),
    // which an ES6 class would refuse
    function Component(props) { this.props = props; this.state = null; }
    Component.prototype.setState = function (s) { this.state = Object.assign({}, this.state, s); };
    const slot = (init) => {
        const r = makeReact.__render;
        if (!r) return init();
        const i = r.cursor++;
        if (!(i in r.slots)) r.slots[i] = init();
        return r.slots[i];
    };
    return {
        Component,
        createContext: (v) => ({ __context: true, Provider: { __provider: true }, value: v }),
        createElement: (type, props, ...children) => ({ type, props: props || {}, children }),
        useContext: () => makeReact.__ctxValue,
        // React subscribes and then reads; a double that only reads would hide a store that is never subscribed to
        useSyncExternalStore: (subscribe, getSnapshot) => { subscribe(() => {}); return getSnapshot(); },
        // the rest of what the registry reaches for while installing; a double, not a stub of behaviour
        memo: (fn) => fn,
        forwardRef: (fn) => fn,
        useRef: (v) => slot(() => ({ current: v })),
        useMemo: (fn) => fn(),
        // memoized by deps, as React does: what a ref callback's deps say decides whether a host is placed again
        useCallback: (fn, deps) => {
            const s = slot(() => ({ deps: null, fn: null }));
            const changed = !s.fn || !deps || !s.deps || deps.length !== s.deps.length
                || deps.some((d, i) => !Object.is(d, s.deps[i]));
            if (changed) { s.fn = fn; s.deps = deps; }
            return s.fn;
        },
        useEffect: (fn, deps) => {
            const s = slot(() => ({ deps: null, ran: false }));
            const changed = !s.ran || !deps || !s.deps || deps.length !== s.deps.length
                || deps.some((d, i) => !Object.is(d, s.deps[i]));
            s.ran = true; s.deps = deps;
            if (changed) makeReact.__effects.push(fn);
        },
        useLayoutEffect: () => {},
        useState: (v) => [v, () => {}],
        Fragment: 'Fragment',
    };
}
// what every root's view answers for a name it keys as itself (ReactRoot.Placement.entryOf's default)
const entryOf = (data, name) => data[name];
function render(instance, fn) {
    makeReact.__render = instance; instance.cursor = 0;
    try { return fn(); } finally { makeReact.__render = null; }
}
const instance = () => ({ slots: [], cursor: 0 });
function flushEffects() {
    const effects = makeReact.__effects.splice(0);
    for (const effect of effects) effect();
}

function load() {
    const sandbox = { console: {
        error: (...a) => sandbox.__errors.push(a.map(String).join(' ')),
        warn: (...a) => sandbox.__warnings.push(a.map(String).join(' ')),
        log: () => {} } };
    sandbox.window = sandbox;
    sandbox.__errors = [];
    sandbox.__warnings = [];
    sandbox.React = makeReact();
    makeReact.__effects = [];
    vm.createContext(sandbox);
    vm.runInContext(containsHtmlTag, sandbox);
    vm.runInContext(registrySrc, sandbox);
    sandbox.lsfusion.__installReactHooks();
    return sandbox;
}

// ---- Caption ---------------------------------------------------------------------------------------------------
console.log('Caption');
{
    const w = load(), Caption = w.lsfusion.Caption;
    const plain = Caption({ value: 'Order 5', className: 'c' });
    check('plain text is printed, not injected', plain.type === 'span' && plain.children[0] === 'Order 5'
        && !plain.props.dangerouslySetInnerHTML, JSON.stringify(plain));
    check('the class is passed through', plain.props.className === 'c');

    const leading = Caption({ value: '<b>Order</b> 5' });
    check('markup at the start is injected', !!leading.props.dangerouslySetInnerHTML);

    // the reason Caption stopped copying Image's leading-'<' test
    const middle = Caption({ value: 'Order <b>#5</b>' });
    check('markup in the MIDDLE is injected', !!middle.props.dangerouslySetInnerHTML,
        JSON.stringify(middle.children));
    check('...and carries the whole value', middle.props.dangerouslySetInnerHTML.__html === 'Order <b>#5</b>');

    check('a comparison is not markup', Caption({ value: 'a < b' }).children[0] === 'a < b');
    check('an entity is not markup', Caption({ value: '&lt;b&gt;' }).children[0] === '&lt;b&gt;');
    check('an empty caption draws nothing', Caption({ value: '' }) === null);
    check('a missing caption draws nothing', Caption({ value: undefined }) === null);
    check('a null caption draws nothing', Caption({ value: null }) === null);
}

// ---- Image -----------------------------------------------------------------------------------------------------
console.log('Image');
{
    const w = load(), Image = w.lsfusion.Image;
    const address = Image({ value: '/static/img/a.png', className: 'i', alt: 'A' });
    check('an address becomes an img', address.type === 'img' && address.props.src === '/static/img/a.png');
    check('the alt is passed through', address.props.alt === 'A');
    check('a missing alt is empty, not undefined', Image({ value: '/a.png' }).props.alt === '');

    const element = Image({ value: '<i class="fa fa-user"></i>' });
    check('a ready element is injected', element.type === 'span' && !!element.props.dangerouslySetInnerHTML);

    check('an empty image draws nothing', Image({ value: '' }) === null);
    check('a null image draws nothing', Image({ value: null }) === null);

    // an image is an address or an element - unlike a caption, a value with a tag in the MIDDLE is not a thing the
    // platform emits, and Image treats it as an address. Stated here so the difference from Caption is deliberate
    const odd = Image({ value: 'a<b>c' });
    check('Image tells the two apart by the leading < only', odd.type === 'img');
}

// ---- the error boundary ----------------------------------------------------------------------------------------
console.log('error boundary');
{
    const w = load(), Boundary = w.lsfusion.__boundary;
    check('the boundary is not enumerable', Object.keys(w.lsfusion).indexOf('__boundary') < 0);
    check('a boundary exists at all', typeof Boundary === 'function');

    const b = new Boundary({ name: 'FormsBoard', children: 'THE COMPONENT' });
    b.state = { failed: null };
    check('it draws its child while nothing failed', b.render() === 'THE COMPONENT');

    const state = Boundary.getDerivedStateFromError(new Error('boom'));
    check('an error becomes state', state && String(state.failed) === 'Error: boom');

    b.state = state;
    const failed = b.render();
    check('the reason replaces the child', failed.type === 'span' && failed.props.className === 'lsf-view-error');
    check('it names the component', failed.children[0].indexOf('FormsBoard') >= 0, failed.children[0]);
    check('it says what was thrown', failed.children[0].indexOf('boom') >= 0, failed.children[0]);

    // a failure is about the data that broke it: the next projection is a new attempt, or one throw would leave the
    // window reading its own error for the rest of the session
    const d1 = { open: [] }, d2 = { open: [] };
    check('the same data leaves the failure standing',
        Boundary.getDerivedStateFromProps({ data: d1 }, { failed: 'e', data: d1 }) === null);
    const cleared = Boundary.getDerivedStateFromProps({ data: d2 }, { failed: 'e', data: d1 });
    check('a new projection clears it', cleared && cleared.failed === null && cleared.data === d2);

    b.componentDidCatch(new Error('boom'));
    check('the console gets the platform prefix and the error', w.__errors.length === 1
        && w.__errors[0].indexOf('lsFusion custom view:') === 0 && w.__errors[0].indexOf('FormsBoard') > 0,
        JSON.stringify(w.__errors));
}

// ---- <Lsf> and the crossing back to the platform ----------------------------------------------------------------
console.log('Lsf');
{
    const w = load();
    // the context every hook reads; the double returns it from useContext
    const crossed = [];
    makeReact.__ctxValue = { view: { entryOf,
        mount: (name, host, row) => crossed.push(['mount', name, row]),
        unmount: (name, host, row) => crossed.push(['unmount', name, row]),
    } };

    const ref = w.lsfusion.useLsf('o.note').ref;
    check('useLsf hands back the props of a host, a ref callback among them', typeof ref === 'function');
    ref({ tag: 'host' });
    check('a host crosses to the platform', crossed.length === 1 && crossed[0][0] === 'mount'
        && crossed[0][1] === 'o.note', JSON.stringify(crossed));
    ref(null);
    check('and the cleanup is its exact inverse', crossed.length === 2 && crossed[1][0] === 'unmount'
        && crossed[1][1] === 'o.note', JSON.stringify(crossed));

    // a place is its name: without one there is nothing to ask the platform for, and the name would arrive as the
    // string "undefined" and be blamed on whatever the window holds
    for (const missing of [undefined, null, '']) {
        const w2 = load();
        makeReact.__ctxValue = { view: { entryOf, mount: () => check('nothing crosses for a nameless <Lsf>', false),
                                         unmount: () => {} } };
        const noRef = w2.lsfusion.useLsf(missing).ref;
        noRef({ tag: 'host' });
        check('a nameless <Lsf> says so: ' + JSON.stringify(missing),
            w2.__errors.length === 1 && w2.__errors[0].indexOf('has no name') > 0, JSON.stringify(w2.__errors));
    }

    // the row path: a per-row renderer has one host per ROW, and unmounting has to name the same row the mount did,
    // even after the row object has been rebuilt - which is why the hook keys on the row's handle and remembers the row
    const w4 = load();
    const seen = [];
    makeReact.__ctxValue = { view: { entryOf, mount: (n, h, r) => seen.push(['mount', n, r && r.key]),
                                     unmount: (n, h, r) => seen.push(['unmount', n, r && r.key]) } };
    const rowA = { key: 'r1', qty: 1 };
    const rowRef = w4.lsfusion.useLsf('o.qty', { row: rowA }).ref;
    rowRef({ tag: 'rowHost' });
    check('a row host crosses with its row', seen.length === 1 && seen[0][2] === 'r1', JSON.stringify(seen));
    rowRef(null);
    check('and is given back naming the SAME row', seen.length === 2 && seen[1][0] === 'unmount' && seen[1][2] === 'r1',
        JSON.stringify(seen));

    // a per-row name is an integration name, which two groups may share, and their rows may share a key: the host is
    // placed again when the row is another group's, and not when the same row is rebuilt - its `objects` handle stays
    const w4b = load();
    makeReact.__ctxValue = { view: { entryOf, mount: () => {}, unmount: () => {} } };
    const hostOf = { cursor: 0, slots: [] };
    const handleA = { group: 'a' }, handleB = { group: 'b' };
    const refA = render(hostOf, () => w4b.lsfusion.useLsf('o.qty', { row: { key: '1', objects: handleA } }).ref);
    const refA2 = render(hostOf, () => w4b.lsfusion.useLsf('o.qty', { row: { key: '1', objects: handleA, qty: 2 } }).ref);
    const refB = render(hostOf, () => w4b.lsfusion.useLsf('o.qty', { row: { key: '1', objects: handleB } }).ref);
    check('the same row rebuilt keeps its host placed', refA === refA2);
    check("another group's row with the same key is placed again", refA2 !== refB);
    // ... and the row's `objects` handle names the host the row itself does: `row={row.objects}` is placed as
    // `row={row}` is, the hook keying on the handle either way
    const refC = render(hostOf, () => w4b.lsfusion.useLsf('o.qty', { row: { key: '1', objects: handleA } }).ref);
    const refH = render(hostOf, () => w4b.lsfusion.useLsf('o.qty', { row: handleA }).ref);
    check('row={row.objects} keeps the host row={row} placed', refC === refH);

    // a second host for the same name while the first still holds it: the hook itself does not judge that - it hands
    // both to the platform, which is where the "first one keeps it" rule lives
    const w5 = load();
    const both = [];
    makeReact.__ctxValue = { view: { entryOf, mount: (n, h) => both.push(h.tag), unmount: () => {} } };
    w5.lsfusion.useLsf('a').ref({ tag: 'h1' });
    w5.lsfusion.useLsf('a').ref({ tag: 'h2' });
    check('two hosts for one name both reach the platform', both.join(',') === 'h1,h2', both.join(','));

    // a name is a name however it was written: the platform keeps its views in a Map, and `name={8}` for the form the
    // projection calls "8" would find nothing under a type-strict key
    const w6 = load();
    const asked = [];
    makeReact.__ctxValue = { view: { entryOf, mount: (n) => asked.push([n, typeof n]), unmount: () => {} } };
    w6.lsfusion.useLsf(8).ref({ tag: 'h' });
    check('a number name reaches the platform as its string', asked.length === 1 && asked[0][0] === '8'
        && asked[0][1] === 'string', JSON.stringify(asked));

    // an <Lsf> outside the root the platform mounted has no way to reach it, and says so instead of throwing
    const w7 = load();
    makeReact.__ctxValue = null; // no Provider above it
    const orphan = w7.lsfusion.useLsf('a', { className: 'c' });
    check('an <Lsf> outside the platform root does not throw', typeof orphan.ref === 'function');
    check('...is not marked, having nothing to hold', orphan.className === 'c' && !('data-lsf-sid' in orphan),
        JSON.stringify(orphan));
    orphan.ref({ tag: 'h' });
    check('...and says why', w7.__errors.length === 1 && w7.__errors[0].indexOf('outside the view') > 0,
        JSON.stringify(w7.__errors));

    // the host never renders React children of its own - the platform owns what goes inside it
    const w3 = load();
    makeReact.__ctxValue = { view: { entryOf, mount: () => {}, unmount: () => {}, marks: true } };
    const element = w3.lsfusion.Lsf({ name: 'a', className: 'c', style: { height: 1 }, children: 'IGNORED' });
    check('Lsf renders a div', element.type === 'div');
    check('...with the class it was given, after its own mark', element.props.className === 'lsf-view c',
        element.props.className);
    check('...naming what it holds', element.props['data-lsf-sid'] === 'a', element.props['data-lsf-sid']);
    check('...with the style it was given', element.props.style && element.props.style.height === 1);
    check('...and never its own children', element.children.filter(c => c !== undefined).length === 0,
        JSON.stringify(element.children));

    // the marks are the host's own props, so a class that changes between renders cannot take them away: React
    // rewrites the whole class attribute from the className prop, and the marks are in it every time
    const w8 = load();
    makeReact.__ctxValue = { view: { entryOf, mount: () => {}, unmount: () => {}, marks: true } };
    const first = w8.lsfusion.Lsf({ name: 'BOX(o)', className: 'panel' });
    const second = w8.lsfusion.Lsf({ name: 'BOX(o)', className: 'panel panel-wide' });
    check('a changed class keeps the mark', first.props.className === 'lsf-view panel'
        && second.props.className === 'lsf-view panel panel-wide', first.props.className + ' / ' + second.props.className);
    check('...and the name', second.props['data-lsf-sid'] === 'BOX(o)');
    check('a host with no class of its own is still marked', w8.lsfusion.Lsf({ name: 'x' }).props.className === 'lsf-view');

    // useLsf puts the same marks on the component's own element, merged with the class it is given
    const own = w8.lsfusion.useLsf('BOX(o)', { className: 'board-panel' });
    check('useLsf merges the class it is given with the mark', own.className === 'lsf-view board-panel', own.className);
    check('...and names what the element holds', own['data-lsf-sid'] === 'BOX(o)', own['data-lsf-sid']);
    check('a number name is marked as its string', w8.lsfusion.useLsf(8)['data-lsf-sid'] === '8');

    // a host that names nothing places nothing, so it is not marked as holding anything
    const nameless = w8.lsfusion.Lsf({ className: 'c' });
    check('a nameless host is not marked', nameless.props.className === 'c' && !('data-lsf-sid' in nameless.props),
        JSON.stringify(nameless.props));

    // the navigator and the log windows leave their hosts as the component rendered them: the class is the
    // component's alone (a form container and the forms window mark theirs)
    const w9 = load();
    makeReact.__ctxValue = { view: { entryOf, mount: () => {}, unmount: () => {}, marks: false } };
    const plain = w9.lsfusion.Lsf({ name: 'Sale.sales', className: 'menu-main' });
    check('a host of a root that marks nothing keeps its own class', plain.props.className === 'menu-main',
        plain.props.className);
    check('...and no name', !('data-lsf-sid' in plain.props));
    check('...nor a class when it was given none', w9.lsfusion.Lsf({ name: 'Sale.sales' }).props.className === undefined);

    // a host is hidden while the entry its name keys says `hidden` - a descriptor, following SHOWIF - and it stays:
    // only `hidden` says so, the ref being the same, so the child is not taken out and goes on being read
    const w10 = load();
    const snapshot = { 'BOX(o)': { caption: 'Orders', hidden: true }, 'BOX(i)': { caption: 'Items' } };
    makeReact.__ctxValue = { view: { entryOf, mount: () => {}, unmount: () => {}, marks: true },
                             store: { subscribe: () => () => {}, getSnapshot: () => snapshot } };
    check('a host whose entry says hidden is hidden', w10.lsfusion.useLsf('BOX(o)').hidden === true);
    check('a host whose entry does not say so is not', w10.lsfusion.useLsf('BOX(i)').hidden === undefined);
    check("a row's host is not hidden by a top-level entry", w10.lsfusion.useLsf('BOX(o)', { row: { key: 'r1' } }).hidden === undefined);
    const hiddenDiv = w10.lsfusion.Lsf({ name: 'BOX(o)', className: 'c', style: { width: 2 } });
    check('<Lsf> hides its div the same way, with its class and style', hiddenDiv.props.hidden === true
        && hiddenDiv.props.style.width === 2 && hiddenDiv.props.className.indexOf('c') >= 0, JSON.stringify(hiddenDiv.props));
    // the root's view says which entry a name keys: a form container keys an lsf panel property's by its integration
    // name on its group's node, and places the property by that same path, o.note
    const keyed = { 'BOX(o)': { caption: 'Orders' }, o: { note: { caption: 'Note', hidden: true } } };
    makeReact.__ctxValue = { view: { mount: () => {}, unmount: () => {}, marks: true,
                                     entryOf: (data, name) => name === 'o.note' ? data.o.note : data[name] },
                             store: { subscribe: () => () => {}, getSnapshot: () => keyed } };
    check("a host is hidden by the entry the root's view says its name keys", w10.lsfusion.useLsf('o.note').hidden === true);
    check('...and a name the view keys as itself is read as itself', w10.lsfusion.useLsf('BOX(o)').hidden === undefined);
    makeReact.__ctxValue = { view: { entryOf, mount: () => {}, unmount: () => {} } }; // a root with no store
    check('a root with no store hides nothing', w10.lsfusion.useLsf('BOX(o)').hidden === undefined);
}

// ---- the hooks an application calls ------------------------------------------------------------------------------
console.log('useData / useController');
{
    const w = load();
    const snapshots = [{ open: ['0'] }, { open: ['0', '1'] }];
    let current = 0, listener = null;
    makeReact.__ctxValue = {
        controller: { select: () => 'selected' },
        store: { subscribe: (l) => { listener = l; return () => { listener = null; }; },
                 getSnapshot: () => snapshots[current] },
    };
    check('useData hands back the whole projection by default', w.lsfusion.useData() === snapshots[0]);
    check('...and what a selector asks for', w.lsfusion.useData((d) => d.open.length) === 1);
    current = 1;
    check('a new snapshot is what the next read returns', w.lsfusion.useData() === snapshots[1]);
    check('useController hands back the controller itself',
        w.lsfusion.useController().select() === 'selected');
    check('the store is subscribed to, not polled', typeof listener === 'function');
}

// ---- the rows helpers --------------------------------------------------------------------------------------------
// a projection as a form container has it: a group whose rows this view draws, one with none right now, and two of
// which it holds only a panel property - nodes without rows, where a property may even be named `keys`
const rowsOfO = { '1': { key: '1', n: 1 }, '2': { key: '2', n: 2 }, '3': { key: '3', n: 3 } };
const formSnapshot = {
    o: { keys: ['1', '2', '3'], list: [rowsOfO['1'], rowsOfO['2'], rowsOfO['3']], byKey: rowsOfO, properties: [] },
    e: { keys: [], list: [], byKey: {}, properties: [] },
    p: { properties: ['qty'], qty: { value: 5 } },
    k: { properties: ['keys'], keys: { value: 5 } },
};
const useFormStore = () => {
    makeReact.__ctxValue = { view: null, store: { subscribe: () => () => {}, getSnapshot: () => formSnapshot } };
};
const Row = function Row() {};
const drawRow = (element) => element.type(element.props); // the wrapper is memo(fn), and the double's memo is fn itself

console.log('List');
{
    const w = load(); useFormStore();
    const all = w.lsfusion.List({ group: 'o', component: Row, extra: 'x' });
    check('<List group> draws a row per key of the group, in its order', Array.isArray(all)
        && all.map(e => e.props.key).join() === '1,2,3', JSON.stringify(all));
    const second = drawRow(all[1]);
    check('...each read out of the store by the group and its key', second.type === Row
        && second.props.row === rowsOfO['2'] && second.props.rowKey === '2' && second.props.index === 1,
        JSON.stringify(second.props));
    check('...with the other props handed on to it', second.props.extra === 'x');
    check("...and none of List's own", !('group' in second.props) && !('component' in second.props)
        && !('keys' in second.props), JSON.stringify(second.props));

    // what reading the store buys: the order can come from anywhere, a spread copy of the node included
    const subset = w.lsfusion.List({ group: 'o', keys: ['3', '1'], component: Row });
    check('keys draws only those rows, in that order', subset.map(e => e.props.key).join() === '3,1');
    check('...each still read out of the store', drawRow(subset[0]).props.row === rowsOfO['3']);
    const copied = w.lsfusion.List({ group: 'o', keys: Object.assign({}, formSnapshot.o).keys, component: Row });
    check('...so a copy of the node draws the same rows', drawRow(copied[2]).props.row === rowsOfO['3']);
    check('a key the group does not hold draws nothing rather than a null row',
        drawRow(w.lsfusion.List({ group: 'o', keys: ['9'], component: Row })[0]) === null);

    const none = w.lsfusion.List({ group: 'e', component: Row });
    check('a group with no rows right now draws no rows and says nothing', Array.isArray(none) && none.length === 0
        && w.__errors.length === 0, JSON.stringify(w.__errors));

    // one List: the page-global switch to another one is gone
    w.lsfusion.listSimple = true;
    const after = w.lsfusion.List({ group: 'o', component: Row });
    check('listSimple changes nothing', after.length === 3 && after[0].type === all[0].type);

    // rows this view does not draw: a mistake in the view, not a state to draw - List throws, naming the group
    for (const group of ['p', 'q', 'k']) {
        let thrown = null;
        try { w.lsfusion.List({ group, component: Row }); } catch (e) { thrown = e; }
        check("<List> over rows not drawn here throws: '" + group + "'", thrown !== null
            && thrown.message === "<List> is given the group '" + group + "', whose rows this view does not draw",
            String(thrown));
    }
    let nameless = null;
    try { w.lsfusion.List({ component: Row }); } catch (e) { nameless = e; }
    check('a <List> with no group throws, saying so', nameless !== null && nameless.message.indexOf('no group') > 0,
        String(nameless));
}

console.log('BucketScope');
{
    const w = load(); useFormStore();
    const scope = w.lsfusion.BucketScope({ group: 'o', bucketOf: (row) => row.n % 2, children: 'CELLS' });
    check('a BucketScope over rows drawn here provides its index', scope.type && scope.type.__provider
        && scope.children[0] === 'CELLS', JSON.stringify(scope));
    check('...bucketed out of the store by the group', scope.props.value.getBucket('1').join() === '1,3'
        && scope.props.value.getBucket('0').join() === '2', JSON.stringify(scope.props.value.getBucket('1')));

    let bad = null;
    try { w.lsfusion.BucketScope({ group: 'p', bucketOf: () => 'x', children: 'CELLS' }); } catch (e) { bad = e; }
    check('a BucketScope over rows not drawn here throws', bad !== null
        && bad.message === "<BucketScope> is given the group 'p', whose rows this view does not draw", String(bad));
    let named = null;
    try { w.lsfusion.BucketScope({ group: 'k', bucketOf: () => 'x', children: 'CELLS' }); } catch (e) { named = e; }
    check("...and over a node whose panel property is named 'keys'", named !== null
        && named.message === "<BucketScope> is given the group 'k', whose rows this view does not draw", String(named));
}

console.log('useSeekOnScroll');
{
    // a DOM double: what the hook reads of an element - its place in the tree and in the document, its overflow and
    // its geometry - and what it may write, its scrollTop and its style
    let order = 0;
    const node = (parent, own) => Object.assign({
        order: order++, parentElement: parent || null, isConnected: true, style: {},
        scrollTop: 0, scrollHeight: 100, clientHeight: 100, overflowY: 'visible', rect: { top: 0, bottom: 10 },
        getBoundingClientRect() { return this.rect; },
        contains(other) { for (let p = other; p; p = p.parentElement) if (p === this) return true; return false; },
        compareDocumentPosition(other) { return other.order > this.order ? 4 : 2; },
    }, own || {});
    const scrolling = { overflowY: 'auto', scrollHeight: 1000, clientHeight: 100, rect: { top: 0, bottom: 100 } };

    // the browser around the hook: the observer it watches the rows with, and the timers it settles on
    const browser = (w) => {
        const timers = [];
        w.getComputedStyle = (e) => ({ overflowY: e.overflowY });
        w.Node = { DOCUMENT_POSITION_FOLLOWING: 4 };
        w.setTimeout = w.requestAnimationFrame = (fn) => timers.push(fn);
        w.innerHeight = 100;
        w.clearTimeout = w.cancelAnimationFrame = (id) => { if (id) timers[id - 1] = null; };
        w.IntersectionObserver = function (callback) { w.__observer = this; this.callback = callback; };
        w.IntersectionObserver.prototype.observe = () => {};
        w.IntersectionObserver.prototype.unobserve = () => {};
        w.IntersectionObserver.prototype.disconnect = () => {};
        return () => { // run what is due, whatever it schedules on the way
            for (let i = 0; i < timers.length; i++) { const fn = timers[i]; if (fn) { timers[i] = null; fn(); } }
        };
    };
    // a mounted card view over five rows, the first one current; `scroller` says where the rows scroll
    const mount = (w, rootOf) => {
        const rowNodes = [], rows = [], seeks = [];
        const root = rootOf();
        for (let k = 1; k <= 5; k++) {
            rowNodes.push(node(root.rowsIn));
            rows.push({ key: String(k), isCurrent: k === 1 });
        }
        makeReact.__ctxValue = root.context;
        const controller = { change: (row) => seeks.push(row.key) };
        const hook = instance();
        const draw = () => {
            const seekRef = render(hook, () => w.lsfusion.useSeekOnScroll(controller));
            rows.forEach((row, i) => seekRef(row)(rowNodes[i])); // the commit hands the refs...
            flushEffects();                                      // ...and runs the effects after it
        };
        draw();
        return { rowNodes, rows, seeks, draw };
    };
    // the user scrolls: the current row leaves the screen, the third and the fourth are on it
    const scrollAway = (w, view, run) => {
        w.__observer.callback([{ target: view.rowNodes[0], isIntersecting: false, intersectionRatio: 0 },
                               { target: view.rowNodes[2], isIntersecting: true, intersectionRatio: 1 },
                               { target: view.rowNodes[3], isIntersecting: true, intersectionRatio: 1 }]);
        run();
    };
    // a click elsewhere makes the last row current, and it is below what the scroller shows
    const currentBelow = (view) => {
        view.rows[0] = { key: '1', isCurrent: false };
        view.rows[4] = { key: '5', isCurrent: true };
        view.rowNodes[4].rect = { top: 150, bottom: 160 };
        view.draw();
    };

    {
        const w = load(), run = browser(w);
        let box;
        const view = mount(w, () => {
            const root = node(null);
            box = node(root, scrolling);
            return { rowsIn: box, context: { element: root, view: null, store: null } };
        });
        scrollAway(w, view, run);
        check('rows scrolling in a box of the view: current is reseated on the edge it left through',
            view.seeks.join() === '3', view.seeks.join());
        check("...and the box's own anchoring is leased away while the hook's holds", box.style.overflowAnchor === 'none');
        currentBelow(view);
        check('...and a current moved from outside is scrolled into view by that box', box.scrollTop === 60, box.scrollTop);
        check('...with nothing in the console', w.__errors.length === 0, JSON.stringify(w.__errors));
    }
    {
        const w = load(), run = browser(w);
        let root;
        const view = mount(w, () => {
            root = node(null, scrolling); // the element the view is mounted in is the view's own, scrolling included
            return { rowsIn: root, context: { element: root, view: null, store: null } };
        });
        scrollAway(w, view, run);
        check('rows scrolling in the element the view is mounted in: the hook follows them', view.seeks.join() === '3'
            && root.style.overflowAnchor === 'none', view.seeks.join());
    }
    {
        const w = load(), run = browser(w);
        let above;
        const view = mount(w, () => {
            above = node(null, scrolling); // a platform container, or a box another view shares
            const root = node(above);
            return { rowsIn: root, context: { element: root, view: null, store: null } };
        });
        scrollAway(w, view, run);
        check('rows scrolling only above the view: no seek is issued', view.seeks.length === 0, view.seeks.join());
        check('...nothing is written above the view', above.style.overflowAnchor === undefined && above.scrollTop === 0,
            JSON.stringify(above.style) + ' ' + above.scrollTop);
        check('...and the hook says why', w.__errors.length === 1
            && w.__errors[0].indexOf('useSeekOnScroll does nothing here') > 0, JSON.stringify(w.__errors));
        scrollAway(w, view, run);
        currentBelow(view);
        check('...a current moved from outside is not scrolled into view by anything above it', above.scrollTop === 0,
            above.scrollTop);
        check('...and it is said once', w.__errors.length === 1, JSON.stringify(w.__errors));
    }
    {
        const w = load(), run = browser(w);
        let above;
        const view = mount(w, () => {
            above = node(null, scrolling);
            const root = node(above);
            return { rowsIn: root, context: { element: root, view: null, store: null } };
        });
        check('rows scrolling only above the view, all on screen: the hook says nothing', w.__errors.length === 0,
            JSON.stringify(w.__errors));
        currentBelow(view);
        check('...a current moved out of sight from outside, before any seek: not scrolled into view, and the hook says'
            + ' why', above.scrollTop === 0 && w.__errors.length === 1
            && w.__errors[0].indexOf('useSeekOnScroll does nothing here') > 0, JSON.stringify(w.__errors));
    }
    {
        const w = load(), run = browser(w);
        let portal;
        const view = mount(w, () => {
            const root = node(null);
            portal = node(null, scrolling); // a portal's box: outside the element the view is mounted in
            return { rowsIn: portal, context: { element: root, view: null, store: null } };
        });
        scrollAway(w, view, run);
        check('rows a portal draws outside the view: no seek is issued, nothing is written', view.seeks.length === 0
            && portal.style.overflowAnchor === undefined && portal.scrollTop === 0, view.seeks.join());
        check('...and the hook says why', w.__errors.length === 1
            && w.__errors[0].indexOf('what scrolls them is outside it') > 0, JSON.stringify(w.__errors));
    }
    {
        const w = load(), run = browser(w);
        const view = mount(w, () => ({ rowsIn: node(null, scrolling), context: null }));
        scrollAway(w, view, run);
        check('a hook outside the view the platform draws does nothing, and says so', view.seeks.length === 0
            && w.__errors.length === 1 && w.__errors[0].indexOf('outside the view') > 0, JSON.stringify(w.__errors));
    }
}

// ---- the registry itself ---------------------------------------------------------------------------------------
console.log('registry');
{
    const w = load(), custom = w.lsfusion.custom;
    const impl = function A() {};
    custom.register('A', impl);
    check('a registered name comes back', custom.get('A') === impl);
    custom.register('A', impl); // the same bundle loaded twice
    check('re-registering the SAME impl is harmless', custom.get('A') === impl);

    let threw = false;
    try { custom.register('A', function () {}); } catch (e) { threw = true; }
    check('a different impl under one name is a hard error', threw);

    check('an unknown name is undefined, not a throw', custom.get('Nope') === undefined);

    // a legacy hand-written global keeps working, and the collision is recorded rather than thrown
    const w2 = load();
    w2.Legacy = function () {};
    const other = function () {};
    w2.lsfusion.custom.register('Legacy', other);
    check('the registry wins over a legacy global', w2.lsfusion.custom.get('Legacy') === other);
    check('the collision is diagnosed', (w2.lsfusion.custom.diagnostics || []).length > 0,
        JSON.stringify(w2.lsfusion.custom.diagnostics));
}

// ---- installing the hooks --------------------------------------------------------------------------------------
console.log('hooks');
{
    const w = load();
    const ctx = w.lsfusion.__context;
    w.lsfusion.__installReactHooks(); // idempotent: called again at every mount
    check('the context survives a second install', w.lsfusion.__context === ctx);
    check('useData and useController are there', typeof w.lsfusion.useData === 'function'
        && typeof w.lsfusion.useController === 'function');
    check('Lsf is there', typeof w.lsfusion.Lsf === 'function');
}

// ---- the client half of the place protocol ----------------------------------------------------------------------
// GwtClientUtils.getLsfPlaceNames reads the names a template asks for. It is JSNI - a string the Java compiler never
// checks - and it has to find exactly what the server's Custom.mapPlaces finds, or the two ends of one protocol drift.
// The expectations below are the same cases CustomTest states on the server side.
console.log('place names (the real regex, read out of the JSNI)');
{
    const clientSrc = fs.readFileSync(REPO + '/web-client/src/main/java/lsfusion/gwt/client/base/GwtClientUtils.java', 'utf8');
    // scraping a source file is only honest if it cannot quietly match the wrong thing, or nothing
    const patterns = clientSrc.match(/new \$wnd\.RegExp\("<" \+ place \+ "([^"]+)", "gi"\)/g) || [];
    check('the place-name expression is found exactly once', patterns.length === 1, patterns.length);
    const pattern = clientSrc.match(/new \$wnd\.RegExp\("<" \+ place \+ "([^"]+)", "gi"\)/)[1].replace(/\\\\/g, '\\');
    const prefixes = clientSrc.match(/String LSF_PLACE = "([^"]+)"/) || [];
    check('the prefix is read from the source too, not assumed', prefixes[1] === 'lsf:', prefixes[1]);
    const names = (template) => {
        const found = new RegExp('<' + prefixes[1] + pattern, 'gi');
        const out = []; let m;
        while ((m = found.exec(template)) !== null) out.push(m[1]);
        return out;
    };

    const cases = [
        ['<div><Lsf:orders></div>', ['orders']],
        ['<Lsf:a>|<Lsf:b>|<Lsf:c>', ['a', 'b', 'c']],
        ['<lsf:orders>', ['orders']],                       // the prefix is matched either way
        ['<LSF:Orders>', ['Orders']],                       // ...and the name is not
        ['<Lsf:a class="x">', ['a']],                       // an attribute ends the name
        ['<Lsf:a >', ['a']],
        ['<Lsf:a\n>', ['a']],
        ['<Lsf:PROPERTY(note).caption>', ['PROPERTY(note).caption']],
        ['<Lsf:a/>', ['a']],                                // a wrong spelling still NAMES something
        ['<Lsf:🙂зайка>', ['🙂зайка']],
        ['plain markup with no places', []],
        ['<lsfoo><lsf-place>', []],                         // only the prefix makes a place
        ['<Lsf:>', []],                                     // no name at all
    ];
    for (const [template, expected] of cases)
        check('names of ' + JSON.stringify(template), JSON.stringify(names(template)) === JSON.stringify(expected),
            JSON.stringify(names(template)));
}

console.log('\n' + passes + ' passed, ' + failures + ' failed');
process.exit(failures ? 1 : 0);
