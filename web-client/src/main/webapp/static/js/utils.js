
var createPlainObject = function () {
    return {};
};

//this var is needed to localize daterangepicker, because GWT does not accept dynamic keys in arrays
var getRanges = function (wnd, rangeToday, rangeYesterday, rangeLast7Days, rangeLast30Days, rangeThisMonth, rangeLastMonth, clear) {
    return {
        [rangeToday]: [wnd.moment(), wnd.moment()],
        [rangeYesterday]: [wnd.moment().subtract(1, 'days'), wnd.moment().subtract(1, 'days')],
        [rangeLast7Days]: [wnd.moment().subtract(6, 'days'), wnd.moment()],
        [rangeLast30Days]: [wnd.moment().subtract(29, 'days'), wnd.moment()],
        [rangeThisMonth]: [wnd.moment().startOf('month'), wnd.moment().endOf('month')],
        [rangeLastMonth]: [wnd.moment().subtract(1, 'month').startOf('month'), wnd.moment().subtract(1, 'month').endOf('month')],
        [clear] : [null, null]
    };
}

var lsfParams = new Map();

var loadCustomScriptIfNoExist = function (url) {
    var scripts = document.getElementsByTagName('script');
    for (var i = scripts.length; i--;) {
        if (scripts[i].src === url)
            return;
    }
    let scriptElement = document.createElement('script');
    scriptElement.setAttribute('src', url);
    document.body.appendChild(scriptElement);
}
