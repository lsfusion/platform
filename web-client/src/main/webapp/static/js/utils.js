
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
