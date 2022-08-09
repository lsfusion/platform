
var createPlainObject = function () {
    return {};
};

//this var is needed to localize daterangepicker, because GWT does not accept dynamic keys in arrays
var getRanges = function (wnd, rangeToday, rangeYesterday, rangeLast7Days, rangeLast30Days, rangeThisMonth, rangePreviousMonth, rangeThisYear, clear) {
    return {
        [rangeToday]: [wnd.moment(), wnd.moment()],
        [rangeYesterday]: [wnd.moment().subtract(1, 'days'), wnd.moment().subtract(1, 'days')],
        [rangeLast7Days]: [wnd.moment().subtract(6, 'days'), wnd.moment()],
        [rangeLast30Days]: [wnd.moment().subtract(29, 'days'), wnd.moment()],
        [rangeThisMonth]: [wnd.moment().startOf('month'), wnd.moment().endOf('month')],
        [rangePreviousMonth]: [wnd.moment().subtract(1, 'month').startOf('month'), wnd.moment().subtract(1, 'month').endOf('month')],
        [rangeThisYear]: [wnd.moment().startOf('year'), wnd.moment().endOf('year')],
        [clear] : [null, null]
    };
}

var getSingleRanges = function (wnd, rangeToday, rangeYesterday, rangeSevenDaysAgo, rangeThirtyDaysAgo, rangeMonthStart, rangePreviousMonthStart, rangeThisYearStart, clear) {
    return {
        [rangeToday]: [wnd.moment()],
        [rangeYesterday]: [wnd.moment().subtract(1, 'days')],
        [rangeSevenDaysAgo]: [wnd.moment().subtract(7, 'days')],
        [rangeThirtyDaysAgo]: [wnd.moment().subtract(30, 'days')],
        [rangeMonthStart]: [wnd.moment().startOf('month')],
        [rangePreviousMonthStart]: [wnd.moment().subtract(1, 'month').startOf('month')],
        [rangeThisYearStart]: [wnd.moment().startOf('year')],
        [clear] : [null]
    };
}

var lsfParams = {};
var lsfFiles = {};

// cookies
function getCookie(name) {
    let matches = document.cookie.match(new RegExp(
        "(?:^|; )" + name.replace(/([\.$?*|{}\(\)\[\]\\\/\+^])/g, '\\$1') + "=([^;]*)"
    ));
    return matches ? decodeURIComponent(matches[1]) : undefined;
}

function setCookie(name, value, options) {
    let updatedCookie = encodeURIComponent(name) + "=" + encodeURIComponent(value);

    for (let optionKey in options) {
        updatedCookie += "; " + optionKey;
        let optionValue = options[optionKey];
        if (optionValue !== true) {
            updatedCookie += "=" + optionValue;
        }
    }

    document.cookie = updatedCookie;
}

function lsfController(callerElement) {
    let controller;
    while (!controller) {
        controller = callerElement.controller;
        callerElement = callerElement.parentElement;
    }

    return controller;
}

function reload() {
    document.location.reload();
}