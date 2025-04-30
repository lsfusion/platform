// save Node.js variables
window._nodeRequire = window.require;
window._nodeModule = window.module;
window._nodeExports = window.exports;
window._nodeProcess = window.process;

// disable Node.js variables; enable in GwtClientUtils.restoreNodeGlobalsElectron
window.require = undefined;
window.module = undefined;
window.exports = undefined;
window.process = undefined;