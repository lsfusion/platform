// lsFusion React Compiler runtime — platform-vendored, loaded after React and before any custom script
// (negative onWebClientInit order). A server-transformed lightweight .jsx component compiled by the React
// Compiler reads its memo cache through window.lsfusion.rcRuntime.c. The implementation is the standard
// react-compiler-runtime polyfill (useMemo-backed cache) against the window.React global: React 19 exposes
// the cache natively (React.__COMPILER_RUNTIME.c), the vendored React 18 falls back to the useMemo polyfill.
(function () {
    var lsfusion = window.lsfusion = window.lsfusion || {};
    var $empty = Symbol.for('react.memo_cache_sentinel');
    lsfusion.rcRuntime = {
        // window.React is read at call (render) time, not load time, so an app-provided React override
        // at a later (less negative) before-system order still wins
        c: function (size) {
            var React = window.React;
            if (React.__COMPILER_RUNTIME && typeof React.__COMPILER_RUNTIME.c === 'function')
                return React.__COMPILER_RUNTIME.c(size);
            return React.useMemo(function () {
                var $ = new Array(size);
                for (var i = 0; i < size; i++)
                    $[i] = $empty;
                $[$empty] = true;
                return $;
            }, []);
        }
    };
})();
