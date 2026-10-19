// UiReactVisibleIT's React views of UiReactVisible.lsf, one per place the platform hides a view. Each hands on to
// window.uiReactVisible.<name> what useVisible() says - `visible`, and `history`, every value it said in turn -,
// `ticks`, the count of a timer of its own that runs only while the view is visible, `running` while it is set, and
// how many times it was mounted and unmounted - the record outlives an instance, so a view made again shows there
(function () {
    function view(name) {
        return function () {
            var visible = window.lsfusion.useVisible();
            var views = window.uiReactVisible = window.uiReactVisible || {};
            var state = views[name] = views[name] || {history: [], ticks: 0, running: false, mounts: 0, unmounts: 0};
            state.visible = visible;
            if (state.history[state.history.length - 1] !== visible)
                state.history.push(visible);
            React.useEffect(function () {
                state.mounts++;
                return function () { state.unmounts++; };
            }, []);
            React.useEffect(function () {
                if (!visible)
                    return undefined;
                state.running = true;
                var timer = setInterval(function () { state.ticks++; }, 100);
                return function () {
                    clearInterval(timer);
                    state.running = false;
                };
            }, [visible]);
            return React.createElement('div', {className: 'ui-react-visible-' + name}, name + ': ' + (visible ? 'visible' : 'hidden'));
        };
    }
    window.UiReactVisibleSecond = view('second');
    window.UiReactVisibleDetails = view('details');
    window.UiReactVisiblePop = view('pop');
    window.UiReactVisibleGated = view('gated');
    window.UiReactVisiblePlain = view('plain');
})();
