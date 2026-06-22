const g = () => (typeof window !== 'undefined' ? window : globalThis).ReactDOM;
export const createRoot = (...a) => g().createRoot(...a);
export default new Proxy(function () {}, { get: (_, p) => { const RD = g(); return RD ? RD[p] : undefined; } });
