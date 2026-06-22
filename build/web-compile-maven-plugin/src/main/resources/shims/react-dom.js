// bare 'react-dom' over the platform window.ReactDOM, so a third-party mvnpm package importing react-dom
// (render / createPortal / findDOMNode) shares the single platform ReactDOM instead of bundling a second copy.
// react-dom/client (createRoot) has its own shim (react-dom-client.js).
const RD = () => (typeof window !== 'undefined' ? window : globalThis).ReactDOM;
const fn = (name) => (...a) => RD()[name](...a);
export default new Proxy(function () {}, {
    get: (_, p) => { const d = RD(); return d ? d[p] : undefined; },
    apply: (_, t, a) => RD().apply(t, a)
});
export const render = fn('render'), hydrate = fn('hydrate'), createPortal = fn('createPortal'),
    findDOMNode = fn('findDOMNode'), unmountComponentAtNode = fn('unmountComponentAtNode'),
    flushSync = fn('flushSync');
