// lsFusion custom-JS registry — platform-vendored, loaded BEFORE compiled bundles and before GWT (negative onWebClientInit order).
// Compiled web/.compiled/*.js bundles call register(name, impl); the GWT client resolves names registry-first with a
// window[name] fallback (so legacy hand-written globals keep working). One namespace shared with the CUSTOM REACT hooks.
// Public surface: lsfusion.custom.register / lsfusion.custom.get. The entry map is closure-local; collision
// diagnostics are exposed non-enumerably (lsfusion.custom.diagnostics) for debugging, not as API.
(function () {
    var ns = window.lsfusion || (window.lsfusion = {});
    if (ns.custom && ns.custom.register) return; // idempotent (defensive: don't clobber if already installed)
    var entries = Object.create(null);
    var diagnostics = [];
    ns.custom = {
        // register a compiled export under its public name. kind is optional (the .lsf call site supplies the
        // expected kind at resolve time); duplicate names are a hard error (auto-load order across bundles is unstable).
        register: function (name, impl, kind) {
            if (name in entries) {
                if (entries[name].impl === impl) return; // the same impl re-registered (e.g. a bundle loaded twice) — harmless
                throw new Error("lsfusion.custom: duplicate registry name '" + name + "'");
            }
            if (window[name] !== undefined && window[name] !== impl) {
                // legacy global with the same name exists and differs — lsFusion resolvers (registry-first) will see
                // the compiled impl while direct window[name] readers keep the legacy one; must be LOUD, not silent.
                var msg = "lsfusion.custom: collision — '" + name + "' exists both as a window global and a compiled registration (registry wins for lsFusion, window stays for direct readers)";
                diagnostics.push(msg);
                console.warn(msg);
            }
            entries[name] = { impl: impl, kind: kind };
            if (window[name] === undefined) window[name] = impl; // back-compat alias (don't overwrite an existing global)
        },
        // resolve registry-first; expectedKind is a soft, call-site-supplied check (registration kind is optional).
        get: function (name, expectedKind) {
            var e = entries[name];
            if (e) {
                if (expectedKind && e.kind && e.kind !== expectedKind)
                    throw new Error("lsfusion.custom: '" + name + "' is kind " + e.kind + ", used as " + expectedKind);
                return e.impl;
            }
            return undefined; // caller falls back to window[name]
        }
    };
    Object.defineProperty(ns.custom, 'diagnostics', { value: diagnostics }); // internal (non-enumerable)
})();

// CUSTOM REACT hooks + <List>, on the same window.lsfusion namespace. DEFINED here (this script loads before any
// compiled bundle), but INSTALLED lazily — by each compiled bundle's preamble (esbuild banner -> __installReactHooks)
// right before the bundle body runs, and by ReactRoot.createRoot at mount for a hand-written global that gets
// no preamble. That ordering is what lets a bundle alias a helper at module TOP — `const List = window.lsfusion.List;`
// — and still bind to the FINAL window.React: an app may override React at a less-negative before-system order (after
// this script but before the bundles / the form), so we must NOT capture React eagerly here. Idempotent; the first
// caller wins. This is the ONLY copy of the install logic; the GWT client calls it, it does not duplicate it.
(function () {
    var ns = window.lsfusion || (window.lsfusion = {});
    if (ns.__installReactHooks) return; // idempotent (defensive: don't redefine)
    ns.__installReactHooks = function () {
        if (ns.__context) return;
        var React = window.React;
        if (!React) return; // eager path may run before React loads (defensive: it is a before-system resource, so normally present)
        var Ctx = React.createContext(null);
        Object.defineProperty(ns, '__context', { value: Ctx }); // internal (non-enumerable)
        // what a component throwing while it renders leaves behind. React tears its whole root down on an uncaught
        // error, so without this a window drawn by a component goes blank with only a stack in the console - and for
        // the forms window that is the whole application, since the open forms sit parked and hidden. The reason goes
        // where the window is instead, the way a placement mistake already says its own reason there.
        // A class component because that is the only thing React lets catch a render error, and written this way
        // because the registry is plain ES5 and has no build step.
        function Boundary(props) {
            React.Component.call(this, props);
            this.state = { failed: null, data: props.data };
        }
        Boundary.prototype = Object.create(React.Component.prototype);
        Boundary.prototype.constructor = Boundary;
        Boundary.getDerivedStateFromError = function (error) { return { failed: error }; };
        // a failure is about THIS data and not about the component: the next projection is a new attempt, so the
        // window comes back as soon as the application state that broke it changes. Without this one throw would
        // leave the window reading its own error for the rest of the session - and for the forms window that is
        // every open form, unreachable until the page is reloaded
        Boundary.getDerivedStateFromProps = function (props, state) {
            if (props.data === state.data)
                return null;
            return { failed: null, data: props.data };
        };
        Boundary.prototype.componentDidCatch = function (error) {
            window.console.error("lsFusion custom view: '" + this.props.name + "' threw while drawing", error);
        };
        Boundary.prototype.render = function () {
            if (!this.state.failed)
                return this.props.children;
            return React.createElement('span', { className: 'lsf-view-error' },
                                       "lsFusion: '" + this.props.name + "' failed to draw: " + this.state.failed);
        };
        Object.defineProperty(ns, '__boundary', { value: Boundary }); // internal (non-enumerable)
        // the data and the controller of whatever root the component is mounted under - these know nothing about forms
        ns.useData = function (selector) {
            var store = React.useContext(Ctx).store;
            var select = selector || function (s) { return s; };
            return React.useSyncExternalStore(store.subscribe, function () { return select(store.getSnapshot()); });
        };
        ns.useController = function () { return React.useContext(Ctx).controller; };
        // the platform's OWN root, mounted once. It READS the projection out of the store instead of having it pushed
        // by a second root.render, and that is what makes a change one React pass: the notification queues this root
        // together with every selector subscriber, and React renders them in one go - whereas a root.render scheduled
        // beside the notification is a second pass it cannot coalesce, the two being on different lanes.
        // props.data stays the primary contract: this root re-renders on every change and builds the component a new
        // props object, so a component that subscribes to nothing is still redrawn. The boundary sits here too, with
        // the same data, so a component that throws still leaves its reason in the window
        var Root = function (props) {
            var data = ns.useData();
            return React.createElement(ns.__boundary, { name: props.name, data: data },
                React.createElement(props.component, { data: data, controller: props.controller }));
        };
        Object.defineProperty(ns, '__root', { value: Root }); // internal (non-enumerable)
        // an image the platform projected - a property of an image class, an icon of a navigator element or of an
        // action. The platform emits either an address or a ready element (a font icon has no address at all), and
        // the two are told apart by the leading '<', which an address never has - so one component draws both, and
        // no view has to inject platform markup itself.
        ns.Image = function (props) {
            var value = props.value;
            if (!value) return null;
            if (value.charAt(0) === '<')
                return React.createElement('span', { className: props.className, style: props.style,
                                                     dangerouslySetInnerHTML: { __html: value } });
            return React.createElement('img', { src: value, className: props.className, style: props.style,
                                                alt: props.alt || '' });
        };
        // a caption the platform computed. Like an image it is "html or text": a plain caption is a string, but a
        // caption the platform built is markup, and printing that as text shows the markup itself. Markup is told
        // from text by the platform's OWN rule (containsHtmlTag - a tag ANYWHERE in the value), not by a leading '<'
        // as an image is: an image is an address or an element, while a caption can carry a tag in the middle of it,
        // so the leading-'<' test would print such a caption as its own source
        ns.Caption = function (props) {
            var value = props.value;
            if (!value) return null;
            if (window.containsHtmlTag(value))
                return React.createElement('span', { className: props.className, style: props.style,
                                                     dangerouslySetInnerHTML: { __html: value } });
            return React.createElement('span', { className: props.className, style: props.style }, value);
        };
        // the crossing BACK to the platform. <Lsf name/> marks where a DESIGN child with `lsf = TRUE` goes:
        // the platform MOVES that child's real GWT view into the host node on mount and back to its park node on
        // cleanup. The host renders NO React children, ever: the moment React owns a child of that node it can wipe the
        // foreign GWT DOM. Cleanup is the exact inverse of mount, so StrictMode's mount->cleanup->mount cannot stack
        // duplicates. The GWT view stays logically attached throughout, so no onUnload/onLoad fires.
        // useLsf(name[, { row, className }]) -> the props of a host, to spread onto an element the component ALREADY
        // renders, so no placeholder node exists at all: the ref that mounts the lsf child into it, the host's marks
        // and, while the child's entry says so, `hidden`.
        // `name` is whatever the root it is mounted under names the thing being placed: the DESIGN identifier of a
        // child for a form container, the canonical name of an element for a navigator window.
        // pass `row` for an LSF grid property: then the name says WHICH property and the row says which of its
        // per-row renderers, so the same name legitimately has one host per row. Pass the row object out of the
        // projected data (or its `objects`) - a key string cannot be resolved back to a row.
        // The marks - the lsf-view class the layout styles a host by, and data-lsf-sid a stylesheet addresses it by -
        // are props of the host's OWN, merged with the className the component gives it, and not something written into
        // the DOM behind React's back: a className is React's, and the render that changes it rewrites the whole class
        // attribute, which took every mark written there with it. So the component hands its class HERE, not to the
        // element after the spread, where it would replace the merged one. Only a root whose owner marks its hosts has
        // them - a form container, the forms window; the navigator's and the log's hosts are left as the component
        // rendered them
        // While the entry the name keys in the root's data says `hidden` - an lsf child's descriptor: its SHOWIF, or a
        // container's above it - the host is `hidden` too, and stays: a child no host places is not read, SHOWIF and
        // all, so it would never come back. Not a row's host: what hides a per-row renderer is its column
        var unsubscribed = function () { return function () {}; }; // a root with no store: nothing is ever hidden
        ns.useLsf = function (name, options) {
            var context = React.useContext(Ctx);
            // an <Lsf> only means anything under the root the platform mounted: a component rendering one into a React
            // root of its own has no way to reach the platform, and used to find that out as a TypeError on `view`
            var view = context ? context.view : null;
            var store = context ? context.store : null;
            var row = options ? options.row : undefined;
            var className = options ? options.className : undefined;
            var held = React.useRef(null); // the ref callback is handed null on detach, so the host is remembered here
            var heldRow = React.useRef(null); // and the row with it, since unmounting has to name the same renderer
            // a name is what an <Lsf> IS, and the platform has the same words for a template place without one -
            // without this the name arrives as the string "undefined" and is reported as something that is not a form.
            // Said from INSIDE the callback, and after the hooks above, because the count of hooks a component calls
            // may not change between renders: an <Lsf> whose name arrives later would otherwise break the component
            var nameless = name === undefined || name === null || name === '';
            // a name is a name whatever it was written as: the platform keeps its views in a Map, whose keys are
            // type-strict, and `name={8}` for the form the projection calls "8" would otherwise find nothing
            var named = nameless ? null : String(name);
            // subscribed whatever the host holds: the count of hooks a component calls may not change between renders
            var hidden = React.useSyncExternalStore(store ? store.subscribe : unsubscribed, function () {
                var data = store && !nameless && row == null ? store.getSnapshot() : null;
                var entry = data ? data[named] : null;
                return !!(entry && entry.hidden === true);
            });
            var ref = React.useCallback(function (host) {
                if (!view) {
                    if (host) window.console.error("lsFusion custom view: an <Lsf> is outside the view the platform draws, so nothing can be placed in it");
                    return;
                }
                if (nameless) {
                    if (host) window.console.error("lsFusion custom view: an <Lsf> has no name, so nothing will ever be placed in it");
                    return;
                }
                if (held.current) { view.unmount(named, held.current, heldRow.current); held.current = null; heldRow.current = null; }
                if (host) {
                    view.mount(named, host, row); held.current = host; heldRow.current = row;
                    // whether this host is in the page can only be told after the commit: React hands the ref while it
                    // renders, and a portal's container is appended by an effect, which runs later. Two frames, because
                    // effects run before the first paint of that commit - anything appended there is in by the second -
                    // and the host is checked only if it is still the one being held, so a mount that was undone
                    // meanwhile is not judged on a node nobody is waiting for
                    if (window.requestAnimationFrame)
                        window.requestAnimationFrame(function () {
                            window.requestAnimationFrame(function () {
                                if (held.current === host) view.check(named, host, row);
                            });
                        });
                }
            }, [view, named, row ? row.key : null]); // by row KEY: the row object is rebuilt whenever its values change
            // a host that names nothing places nothing, so it is not marked as holding anything either
            var marked = !!(view && view.marks && !nameless);
            var host = { ref: ref, className: marked ? (className ? 'lsf-view ' + className : 'lsf-view') : className };
            if (marked)
                host['data-lsf-sid'] = named;
            if (hidden)
                host.hidden = true;
            return host;
        };
        // <Lsf name row className style/> is useLsf on a <div> of its own: the same props, the same marks
        ns.Lsf = function (props) {
            var host = ns.useLsf(props.name, props);
            host.style = props.style;
            return React.createElement('div', host);
        };
        // A lsf child's caption / image is NOT drawn by the platform - it is handed to React in its own entry, keyed as
        // the child is: data[componentSID] for a container or a panel property, data.<group>[integrationSID] for a
        // table one; an ACTION has none, its caption and image being its buttons' face. The component names each child
        // it wants and draws that caption itself; a container is not iterated for you, so a child no Lsf mentions is
        // not shown.

        // one row of a <List>, read out of the root's own store by the group's name and the row's key, so that a change
        // of one row's values redraws that row alone
        var RowWrapper = React.memo(function (p) {
            var row = ns.useData(function (s) { var g = s && s[p.group]; var bk = g && g.byKey; return bk ? bk[p.rowKey] : null; });
            // a key the group does not hold - a row removed (about to unmount), or a subset naming a row that is not
            // there: either way no null row reaches the component
            if (row == null) return null;
            var rowProps = {};
            var pass = p.pass;
            if (pass) for (var pk in pass) rowProps[pk] = pass[pk];
            rowProps.row = row; rowProps.rowKey = p.rowKey; rowProps.index = p.index;
            return React.createElement(p.component, rowProps);
        });
        var isGroupName = function (group) { return group !== undefined && group !== null && group !== ''; };
        // the node's `keys`, or undefined where this view does not draw the group's rows. Only an ARRAY is the rows':
        // where they are drawn elsewhere a panel property may take the name (FormView.getReservedNodeNames), and its
        // entry is an object
        var rowKeysOf = function (snapshot, group) {
            var g = snapshot && isGroupName(group) ? snapshot[group] : null;
            return g && Array.isArray(g.keys) ? g.keys : undefined;
        };
        // A group's rows are projected into the container that draws THEM, and nowhere else: a view that holds only a
        // panel property of the group has a node without rows, and a view that holds nothing of it has no node at all.
        // Whether a view draws a group's rows is the form's design, not its data, so a rows helper asked for rows that
        // are not here is a mistake in the view rather than a state to draw: it throws, and the view's boundary shows
        // the reason in its place and in the console. Asked after the helper's hooks, whose count may not change.
        // The rows are drawn here exactly when the node carries the array `keys`, however many there are: a group with
        // no rows right now has `keys` = [], not no `keys` (rowKeysOf).
        var checkRowsDrawn = function (kind, group, drawn) {
            if (!isGroupName(group))
                throw new Error("<" + kind + "> is given no group, so it has no rows to draw: name the group, as group=\"o\"");
            if (!drawn)
                throw new Error("<" + kind + "> is given the group '" + group + "', whose rows this view does not draw");
        };
        // <List group="o" component={Row} [keys={subset}]/>: the rows of a group, each through a memoized wrapper bound
        // to that one row. They are read out of the root's OWN store by the group's name - what BucketScope reads too -
        // and not out of whatever object the view hands over, so nothing about the node is a condition: the order is
        // the group's `keys`, or `keys` given here, which draws only those rows, in that order
        ns.List = function (props) {
            var comp = props.component || props.children;
            var group = props.group;
            var drawn = ns.useData(function (s) { return rowKeysOf(s, group); });
            var keys = props.keys || drawn || EMPTY;
            var pass = null, deps = [keys, comp, group], pk = [];
            for (var k in props) if (k !== 'group' && k !== 'keys' && k !== 'component' && k !== 'children') pk.push(k);
            pk.sort();
            for (var pi = 0; pi < pk.length; pi++) { (pass || (pass = {}))[pk[pi]] = props[pk[pi]]; deps.push(pk[pi]); deps.push(props[pk[pi]]); }
            var rows = React.useMemo(function () {
                return keys.map(function (rowKey, index) {
                    return React.createElement(RowWrapper, { key: rowKey, group: group, rowKey: rowKey, index: index, component: comp, pass: pass });
                });
            }, deps);
            checkRowsDrawn('List', group, drawn !== undefined);
            return rows;
        };
        // useSeekOnScroll(groupController[, options]) -> (row) => ref callback. The native grid's scroll pact
        // (DataGrid.checkSelectedRowVisible), ported to a custom card view: while the CURRENT row is on screen,
        // scrolling seeks NOTHING; when it leaves, it is reseated on the visible edge it left through - first visible
        // when it left above, last visible when it left below. Since the key window is a contiguous stretch of one
        // linear order, moving current is also what pulls the next page, so the rows (and their <Lsf> editors) follow.
        //   const seekRef = useSeekOnScroll(controller.b, { enabled: follow });
        //   <div key={row.key} ref={seekRef(row)} ...>
        // The other half of the pact, also the grid's: on a window shift the JUST-SEEKED row holds its on-screen
        // position (the scroller is corrected by exactly the layout shift - and the browser's own scroll anchoring is
        // turned off there, or it would correct the same removal a second time); and a current moved from OUTSIDE (a
        // click elsewhere, a programmatic seek) is scrolled INTO view instead of being re-seeked away.
        // The pact rests on the grid's own geometry contract, which is the DEVELOPER's to keep: the page must hold
        // more rows than the viewport can show (set PAGESIZE on the OBJECTS block accordingly). The window then always
        // has a page beyond current, so current leaves the viewport - and reseats, recentring the window - before the
        // scroller can starve at a loaded edge. A page smaller than the viewport stalls there, as the grid itself would.
        // "On screen" is threshold-based (60% by default), a deliberate hysteresis for tall cards where the native
        // grid's touch-the-border rule would churn current on every pixel of clipping.
        // ONE hook per scrolling element: two instances above one scroller would fight over its correction.
        // The scroller is the view's OWN: the hook writes its position and leases its scroll anchoring away, and
        // outside the element the view's root is mounted in every box belongs to something else - a platform
        // container, a box that another view shares, the page, a portal's. So the walk up from a row ends at that
        // element, and rows that scroll only in something outside it leave the hook inert - no seek, no scroll into
        // view, no write - which it says once, in the console.
        // options: enabled (true), threshold (0.6), settle (250ms), onSeek(row) - called for each issued seek.
        ns.useSeekOnScroll = function (groupController, options) {
            var opts = options || {};
            var enabled = opts.enabled == null ? true : !!opts.enabled;
            var context = React.useContext(Ctx);
            var st = React.useRef(null);
            if (st.current == null)
                st.current = { io: null, rows: new Map(), refs: new Map(), onScreen: new Map(),
                               settle: null, flight: null, raf: null,
                               asked: null, busy: false, anchor: null,
                               controller: null, opts: null, lastCurrentKey: null, root: null, saidInert: false };
            var t = st.current;
            t.controller = groupController; t.opts = opts; // read latest from the observer callbacks, no re-subscribe
            t.root = context ? context.element : null; // the element the root is mounted in: nothing above it is ours

            // the nearest element that scrolls the row, looked for from the row up to the root's element and no
            // further; null when there is none there - and a row outside that element (a portal) has none by definition
            var scrollerOf = function (el) {
                var root = t.root;
                if (!root || !root.contains(el)) return null;
                for (var p = el.parentElement; p; p = p.parentElement) {
                    var o = getComputedStyle(p).overflowY;
                    if ((o === 'auto' || o === 'scroll') && p.scrollHeight > p.clientHeight) return p;
                    if (p === root) break;
                }
                return null;
            };
            // what the hook does when the scroll it would follow is not the view's own: nothing, and it says so once
            var inert = function () {
                if (t.saidInert) return;
                t.saidInert = true;
                window.console.error("lsFusion custom view: useSeekOnScroll does nothing here: " + (t.root
                    ? "the rows do not scroll inside the element this view is drawn in - what scrolls them is outside it"
                      + " (a platform container, a box shared with another view, the page, a portal's box), and nothing"
                      + " outside that element is this view's to move. Give the rows a scrolling box of the view's own"
                    : "it is outside the view the platform draws, so it has no scrolling of that view to follow"));
            };
            // the element's LAYOUT position inside the scroller: rect difference + scrollTop cancels the scrolling out,
            // so it changes only when the content above it changes - exactly the shift the correction must undo.
            var layoutTop = function (scroller, el) {
                return el.getBoundingClientRect().top - scroller.getBoundingClientRect().top + scroller.scrollTop;
            };
            // the anchor is ONE invariant: this element, at this layout position, in this scroller (whose own
            // anchoring is leased away while ours holds). Dropping it restores what the lease took.
            var dropAnchor = function () {
                var a = t.anchor;
                if (a) { a.scroller.style.overflowAnchor = a.prevOverflowAnchor; t.anchor = null; }
            };
            var setAnchor = function (el, scroller) {
                if (!t.anchor || t.anchor.scroller !== scroller) {
                    dropAnchor();
                    t.anchor = { scroller: scroller, prevOverflowAnchor: scroller.style.overflowAnchor };
                    // OURS is the only anchoring here: the browser's own would correct the same removal a second
                    // time (measured: overshooting the scroller to 0 and killing the scroll entirely)
                    scroller.style.overflowAnchor = 'none';
                }
                t.anchor.el = el;
                t.anchor.top = layoutTop(scroller, el);
            };
            var compensate = function () {
                var a = t.anchor;
                if (!t.io || !a || !a.el || !a.el.isConnected) return; // disabled -> hands off the scroller
                if (scrollerOf(a.el) !== a.scroller) { dropAnchor(); return; } // the layout moved it elsewhere: stale
                var top = layoutTop(a.scroller, a.el);
                var delta = top - a.top;
                if (delta !== 0) a.scroller.scrollTop += delta; // the seeked row stays where the eye left it
                a.top = top;
            };
            var currentEntry = function () { // the current row's element, from the freshest row objects
                var found = null;
                t.rows.forEach(function (row, el) { if (row.isCurrent && el.isConnected) found = { el: el, row: row }; });
                return found;
            };

            var threshold = opts.threshold == null ? 0.6 : opts.threshold;
            React.useEffect(function () {
                if (!enabled) return undefined;
                var io = new IntersectionObserver(function (entries) {
                    for (var i = 0; i < entries.length; i++) {
                        var e = entries[i];
                        // isIntersecting alone is true at a single pixel; ON SCREEN here means the threshold's worth
                        if (e.isIntersecting && e.intersectionRatio >= threshold) t.onScreen.set(e.target, true);
                        else t.onScreen.delete(e.target);
                    }
                    clearTimeout(t.settle);
                    t.settle = setTimeout(t.evaluate, opts.settle == null ? 250 : opts.settle);
                }, { threshold: threshold });
                t.io = io;
                t.rows.forEach(function (_, el) { io.observe(el); }); // elements mounted before enabling
                return function () { // every pending continuation belongs to THIS lifecycle: none may cross into the next
                    clearTimeout(t.settle); t.settle = null;
                    clearTimeout(t.flight); t.flight = null; t.busy = false; t.asked = null;
                    if (t.raf != null) { cancelAnimationFrame(t.raf); t.raf = null; }
                    io.disconnect(); t.io = null; t.onScreen.clear();
                    dropAnchor();
                    t.lastCurrentKey = null; // on re-enable the standing current is news again: it syncs, not seeks
                };
            }, [enabled, threshold, opts.settle]);

            t.evaluate = function () {
                if (!t.io) return;
                var visible = [];
                t.onScreen.forEach(function (_, el) { if (el.isConnected && t.rows.has(el)) visible.push(el); });
                if (!visible.length) return;
                visible.sort(function (a, b) { // reading order = DOM order
                    return (a.compareDocumentPosition(b) & Node.DOCUMENT_POSITION_FOLLOWING) ? -1 : 1;
                });

                var cur = currentEntry();
                if (!cur) return; // rows in transition (no current delivered yet): the pact has no subject
                if (t.onScreen.has(cur.el)) return; // current on screen: scrolling seeks NOTHING (asked is cleared by
                                                    // the LANDING, not here: a mid-flight scroll reversal must not
                                                    // reclassify our own seek's arrival as an external one)
                // the native exit rule: reseat current on the visible edge it left through
                var target = (cur.el.compareDocumentPosition(visible[0]) & Node.DOCUMENT_POSITION_FOLLOWING)
                        ? visible[0] : visible[visible.length - 1];

                var row = t.rows.get(target);
                if (!row) return;
                if (t.busy || t.asked === row.key) return; // one seek in flight, never re-ask the one in question
                // a seek shifts the rows under the scroller, and only a scroller of the view's own may be corrected for
                // it - one above would be left jumping, so where there is none the seek is not issued at all
                var scroller = scrollerOf(target);
                if (!scroller) { inert(); return; }
                setAnchor(target, scroller); // the new current holds its on-screen place through the coming shift
                t.asked = row.key; t.busy = true;
                t.controller.change(row);
                if (t.opts.onSeek) t.opts.onSeek(row);
                clearTimeout(t.flight); // one seek in flight: let the answer land before the next one
                t.flight = setTimeout(function () { t.flight = null; t.busy = false; }, 500);
            };

            // the outside-change half: current moved NOT through this hook (a click on another row, a programmatic
            // seek) - bring it into view, the way the grid follows its selection; our own seek is only bookkept
            React.useEffect(function () {
                var cur = currentEntry();
                var key = cur ? cur.row.key : null;
                if (key === t.lastCurrentKey) return undefined;
                t.lastCurrentKey = key;
                if (key != null && key === t.asked) { t.asked = null; return undefined; } // our seek landed
                if (cur && enabled) {
                    var sc = scrollerOf(cur.el);
                    // nothing of the view's own scrolls the row: if it is out of sight, the hook cannot bring it in
                    if (!sc) {
                        var seen = cur.el.getBoundingClientRect();
                        if (seen.top < 0 || seen.bottom > window.innerHeight)
                            inert();
                        return undefined;
                    }
                    var r = cur.el.getBoundingClientRect(), b = sc.getBoundingClientRect();
                    // adjust ONLY the chosen scroller (scrollIntoView would also move any ancestor it liked)
                    if (r.top < b.top) sc.scrollTop += r.top - b.top;
                    else if (r.bottom > b.bottom) sc.scrollTop += r.bottom - b.bottom;
                }
                return undefined;
            });

            // one ref callback per row KEY, held so a re-render does not detach/re-attach every element; the row
            // object itself is refreshed on each call (it is rebuilt whenever its values change). Mounts and unmounts
            // are also the signal that the layout under the scroller changed: one correction per commit, after it.
            return function (row) {
                var key = row.key;
                var held = t.refs.get(key);
                if (!held) {
                    held = { el: null, row: null, cb: function (el) {
                        if (held.el) { t.rows.delete(held.el); t.onScreen.delete(held.el); if (t.io) t.io.unobserve(held.el); }
                        held.el = el;
                        if (el) { t.rows.set(el, held.row); if (t.io) t.io.observe(el); }
                        else t.refs.delete(key);
                        if (t.raf == null)
                            t.raf = requestAnimationFrame(function () { t.raf = null; compensate(); });
                    } };
                    t.refs.set(key, held);
                }
                held.row = row;
                if (held.el) t.rows.set(held.el, row); // keep the freshest row object for an already-mounted element
                return held.cb;
            };
        };

        // <BucketScope>/useBucket/<Buckets>: the pivot/matrix analogue of <List> (bucketKey -> rowKey[] index with
        // per-cell subscription).
        var EMPTY = Object.freeze([]);
        var BucketCtx = React.createContext(null);
        var makeBucketStore = function (formStore, groupSID, bucketOf) {
            // ref-diff index: O(rows) ref-compares per form change, re-bucketing only the changed rows
            var listeners = new Set(), buckets = Object.create(null), rowCache = Object.create(null), lastNode, lastKeys = EMPTY, formUnsub = null, pending = false;
            // an ARRAY means several buckets, not a composite key: each element is coerced on its own and nothing is
            // ever joined. The coercion is `'' + value` because a bucket is an object key, so a cell named 1 and one
            // named '1' are one cell - the keys are the author's own values, not the platform's row identities.
            // Bucketing BY a row key composes anyway: String(row.key) IS the row's canonical key string.
            var norm = function (bk) { // bucketOf result -> deduped string[] | null (null = the row lands nowhere)
                if (bk == null) return null;
                if (!Array.isArray(bk)) return ['' + bk];
                var r = [];
                for (var i = 0; i < bk.length; i++) if (bk[i] != null) { var c = '' + bk[i]; if (r.indexOf(c) < 0) r.push(c); }
                return r.length ? r : null;
            };
            var same = function (a, b) { // element-wise INCLUDING order: cell arrays follow the group order
                if (a === b) return true;
                if (!a || !b || a.length !== b.length) return false;
                for (var i = 0; i < a.length; i++) if (a[i] !== b[i]) return false;
                return true;
            };
            var ensure = function () { // sync the index to the current snapshot
                var s = formStore.getSnapshot();
                var g = s ? s[groupSID] : null;
                if (g === lastNode) return;
                var keys = (g && g.keys) || EMPTY, byKey = (g && g.byKey) || null;
                var all = keys !== lastKeys, dirty = all ? null : Object.create(null), any = all;
                var mark = function (b) { if (b && !all) { any = true; for (var i = 0; i < b.length; i++) dirty[b[i]] = true; } };
                var next = Object.create(null), rk, i;
                for (i = 0; i < keys.length; i++) {
                    rk = keys[i];
                    var row = byKey ? byKey[rk] : null;
                    var prev = rowCache[rk];
                    if (prev && prev.ref === row) { next[rk] = prev; continue; } // unchanged ref -> same bucket, skip
                    var nb = row == null ? null : norm(bucketOf(row, rk));
                    if (!prev || !same(prev.b, nb)) { mark(prev && prev.b); mark(nb); }
                    next[rk] = { ref: row, b: nb };
                }
                for (rk in rowCache) if (!(rk in next)) mark(rowCache[rk].b); // removed rows leave their old cells
                // commit only below the last bucketOf call: a throw above leaves the whole index at the previous
                // snapshot, so the next ensure RETRIES instead of early-returning over a half-advanced state.
                // pending: a render-time ensure (getBucket) may consume the change BEFORE the form listener runs; the flag survives to it
                lastNode = g; lastKeys = keys; rowCache = next; pending = true;
                if (any) { // rebuild ONLY the dirty cells' arrays, membership in group (keys) order
                    var acc = Object.create(null);
                    for (i = 0; i < keys.length; i++) {
                        var e = next[keys[i]], b = e && e.b;
                        if (b) for (var j = 0; j < b.length; j++) { var c = b[j]; if (all || dirty[c]) (acc[c] || (acc[c] = [])).push(keys[i]); }
                    }
                    if (all) {
                        for (var c2 in acc) if (same(buckets[c2], acc[c2])) acc[c2] = buckets[c2]; // keep unchanged refs
                        buckets = acc;
                    } else
                        for (var c3 in dirty) {
                            var arr = acc[c3];
                            if (!arr) delete buckets[c3]; // emptied cell -> back to the stable EMPTY
                            else if (!same(buckets[c3], arr)) buckets[c3] = arr;
                        }
                }
            };
            return {
                subscribe: function (l) {
                    listeners.add(l);
                    if (listeners.size === 1) formUnsub = formStore.subscribe(function () { ensure(); if (pending) { pending = false; listeners.forEach(function (x) { x(); }); } }); // unchanged cells then skip by Object.is on their array
                    return function () { listeners['delete'](l); if (!listeners.size && formUnsub) { formUnsub(); formUnsub = null; } };
                },
                getBucket: function (k) { ensure(); return buckets[k] || EMPTY; } // ensure: a cell renders before its effect subscribes
            };
        };
        ns.BucketScope = function (props) { // props: group (SID), bucketOf(row, rowKey) -> bucketKey | bucketKey[] | null, bucketDeps
            var formStore = React.useContext(Ctx).store;
            var group = props.group;
            // whether the rows are drawn here, not the rows themselves: the cells subscribe to those on their own
            var drawn = ns.useData(function (s) { return rowKeysOf(s, group) !== undefined; });
            // bucketOf is CAPTURED at store creation (see makeBucketStore above)
            var bucketOf = props.bucketOf;
            var store = React.useMemo(function () {
                return makeBucketStore(formStore, group, bucketOf);
            }, [formStore, group].concat(props.bucketDeps || EMPTY));
            checkRowsDrawn('BucketScope', group, drawn);
            return React.createElement(BucketCtx.Provider, { value: store }, props.children);
        };
        ns.useBucket = function (bucketKey) { // one call per cell component, for its FIXED key (hook rules)
            var store = React.useContext(BucketCtx);
            if (!store) throw new Error("lsfusion.useBucket: no enclosing BucketScope");
            var key = '' + bucketKey;
            return React.useSyncExternalStore(store.subscribe, function () { return store.getBucket(key); });
        };
        var CellWrapper = React.memo(function (p) {
            var rowKeys = ns.useBucket(p.cellKey);
            var cellProps = {};
            var pass = p.pass;
            if (pass) for (var pk in pass) cellProps[pk] = pass[pk];
            cellProps.cellKey = p.cellKey; cellProps.rowKeys = rowKeys; cellProps.index = p.index;
            return React.createElement(p.component, cellProps);
        });
        var BucketCells = function (props) {
            var comp = props.component || props.children;
            var cells = props.cells || EMPTY;
            var pass = null, deps = [cells, comp], pk = [];
            for (var k in props) if (k !== 'cells' && k !== 'component' && k !== 'children' && k !== 'group' && k !== 'bucketOf' && k !== 'bucketDeps') pk.push(k);
            pk.sort();
            for (var pi = 0; pi < pk.length; pi++) { (pass || (pass = {}))[pk[pi]] = props[pk[pi]]; deps.push(pk[pi]); deps.push(props[pk[pi]]); }
            return React.useMemo(function () {
                return cells.map(function (cellKey, index) {
                    return React.createElement(CellWrapper, { key: '' + cellKey, cellKey: '' + cellKey, index: index, component: comp, pass: pass });
                });
            }, deps);
        };
        ns.Buckets = function (props) {
            return React.createElement(ns.BucketScope, { group: props.group, bucketOf: props.bucketOf, bucketDeps: props.bucketDeps },
                React.createElement(BucketCells, props));
        };
    };
})();
