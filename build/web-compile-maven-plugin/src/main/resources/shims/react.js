// Lazy external-React shim: function exports resolve window.React at USE time, so a bundle whose module-level
// code only registers can load before React. Value exports (Fragment = a Symbol, Component = a class, Children)
// CANNOT be lazy stand-ins — they are read eagerly, which is fine because React is required to load before
// compiled bundles anyway (before-system onWebClientInit order; module-level memo()/class components need it too).
const g = () => (typeof window !== 'undefined' ? window : globalThis).React;
const fn = (name) => (...a) => g()[name](...a);                       // lazy function passthrough
export default new Proxy(function () {}, {
    get: (_, p) => { const R = g(); return R ? R[p] : undefined; },  // React.createElement / React.memo / React.Fragment ...
    apply: (_, t, a) => g().apply(t, a)
});
export const createElement = fn('createElement'), cloneElement = fn('cloneElement'),
    createContext = fn('createContext'), forwardRef = fn('forwardRef'), memo = fn('memo'),
    useState = fn('useState'), useEffect = fn('useEffect'), useLayoutEffect = fn('useLayoutEffect'),
    useRef = fn('useRef'), useMemo = fn('useMemo'), useCallback = fn('useCallback'),
    useContext = fn('useContext'), useReducer = fn('useReducer'), useSyncExternalStore = fn('useSyncExternalStore');
const R0 = g() || {};
export const Children = R0.Children, Fragment = R0.Fragment, Component = R0.Component;
