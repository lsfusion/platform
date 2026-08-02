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
function makeReact() {
    // an ES5 function, exactly as React defines it - the registry's boundary does React.Component.call(this, props),
    // which an ES6 class would refuse
    function Component(props) { this.props = props; this.state = null; }
    Component.prototype.setState = function (s) { this.state = Object.assign({}, this.state, s); };
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
        useRef: (v) => ({ current: v }),
        useMemo: (fn) => fn(),
        useCallback: (fn) => fn,
        useEffect: () => {},
        useLayoutEffect: () => {},
        useState: (v) => [v, () => {}],
        Fragment: 'Fragment',
    };
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
    makeReact.__ctxValue = { view: {
        mount: (name, host, row) => crossed.push(['mount', name, row]),
        unmount: (name, host, row) => crossed.push(['unmount', name, row]),
    } };

    const ref = w.lsfusion.useLsf('PROPERTY(note)', null);
    check('useLsf hands back a ref callback', typeof ref === 'function');
    ref({ tag: 'host' });
    check('a host crosses to the platform', crossed.length === 1 && crossed[0][0] === 'mount'
        && crossed[0][1] === 'PROPERTY(note)', JSON.stringify(crossed));
    ref(null);
    check('and the cleanup is its exact inverse', crossed.length === 2 && crossed[1][0] === 'unmount'
        && crossed[1][1] === 'PROPERTY(note)', JSON.stringify(crossed));

    // a place is its name: without one there is nothing to ask the platform for, and the name would arrive as the
    // string "undefined" and be blamed on whatever the window holds
    for (const missing of [undefined, null, '']) {
        const w2 = load();
        makeReact.__ctxValue = { view: { mount: () => check('nothing crosses for a nameless <Lsf>', false),
                                         unmount: () => {} } };
        const noRef = w2.lsfusion.useLsf(missing, null);
        noRef({ tag: 'host' });
        check('a nameless <Lsf> says so: ' + JSON.stringify(missing),
            w2.__errors.length === 1 && w2.__errors[0].indexOf('has no name') > 0, JSON.stringify(w2.__errors));
    }

    // the row path: a per-row renderer has one host per ROW, and unmounting has to name the same row the mount did,
    // even after the row object has been rebuilt - which is why the hook keys on the row's key and remembers the row
    const w4 = load();
    const seen = [];
    makeReact.__ctxValue = { view: { mount: (n, h, r) => seen.push(['mount', n, r && r.key]),
                                     unmount: (n, h, r) => seen.push(['unmount', n, r && r.key]) } };
    const rowA = { key: 'r1', qty: 1 };
    const rowRef = w4.lsfusion.useLsf('PROPERTY(qty)', rowA);
    rowRef({ tag: 'rowHost' });
    check('a row host crosses with its row', seen.length === 1 && seen[0][2] === 'r1', JSON.stringify(seen));
    rowRef(null);
    check('and is given back naming the SAME row', seen.length === 2 && seen[1][0] === 'unmount' && seen[1][2] === 'r1',
        JSON.stringify(seen));

    // a second host for the same name while the first still holds it: the hook itself does not judge that - it hands
    // both to the platform, which is where the "first one keeps it" rule lives
    const w5 = load();
    const both = [];
    makeReact.__ctxValue = { view: { mount: (n, h) => both.push(h.tag), unmount: () => {} } };
    w5.lsfusion.useLsf('a', null)({ tag: 'h1' });
    w5.lsfusion.useLsf('a', null)({ tag: 'h2' });
    check('two hosts for one name both reach the platform', both.join(',') === 'h1,h2', both.join(','));

    // a name is a name however it was written: the platform keeps its views in a Map, and `name={8}` for the form the
    // projection calls "8" would find nothing under a type-strict key
    const w6 = load();
    const asked = [];
    makeReact.__ctxValue = { view: { mount: (n) => asked.push([n, typeof n]), unmount: () => {} } };
    w6.lsfusion.useLsf(8, null)({ tag: 'h' });
    check('a number name reaches the platform as its string', asked.length === 1 && asked[0][0] === '8'
        && asked[0][1] === 'string', JSON.stringify(asked));

    // an <Lsf> outside the root the platform mounted has no way to reach it, and says so instead of throwing
    const w7 = load();
    makeReact.__ctxValue = null; // no Provider above it
    const orphan = w7.lsfusion.useLsf('a', null);
    check('an <Lsf> outside the platform root does not throw', typeof orphan === 'function');
    orphan({ tag: 'h' });
    check('...and says why', w7.__errors.length === 1 && w7.__errors[0].indexOf('outside the view') > 0,
        JSON.stringify(w7.__errors));

    // the host never renders React children of its own - the platform owns what goes inside it
    const w3 = load();
    makeReact.__ctxValue = { view: { mount: () => {}, unmount: () => {} } };
    const element = w3.lsfusion.Lsf({ name: 'a', className: 'c', children: 'IGNORED' });
    check('Lsf renders a div with the class it was given', element.type === 'div' && element.props.className === 'c');
    check('...and never its own children', element.children.filter(c => c !== undefined).length === 0,
        JSON.stringify(element.children));
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
