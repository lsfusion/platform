// react/jsx-runtime + react/jsx-dev-runtime over the platform window.React (classic createElement), so a
// third-party mvnpm package built with the automatic JSX runtime resolves to the single platform React instead
// of esbuild trying to load react.js/jsx-runtime (the bare 'react' alias would otherwise rewrite the subpath).
const R = () => (typeof window !== 'undefined' ? window : globalThis).React;
function jsx(type, props, key) {
    const p = props || {};
    const children = p.children;
    const rest = {};
    for (const k in p) if (k !== 'children') rest[k] = p[k];
    if (key !== undefined) rest.key = key;
    return R().createElement(type, rest, children);
}
export { jsx, jsx as jsxs, jsx as jsxDEV };
export const Fragment = (R() || {}).Fragment;
