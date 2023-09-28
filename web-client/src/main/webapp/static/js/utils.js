
var createPlainObject = function () {
    return {};
};

//this var is needed to localize daterangepicker, because GWT does not accept dynamic keys in arrays
var getRanges = function (wnd, rangeIntervalToday, rangeIntervalYesterday, rangeLast7Days, rangeLast30Days, rangeThisMonth, rangeToMonthEnd,
                          rangePreviousMonth, rangeMonthStartToCurrentDate, rangeThisYear, rangeToYearEnd, clear, preDefinedDateRangesNames) {
    let ranges = {};
    for (let i = 0; i < preDefinedDateRangesNames.length; i++) {
        let preDefinedDateRangesName = preDefinedDateRangesNames[i];
        if (preDefinedDateRangesName === 'rangeIntervalToday')
            ranges[rangeIntervalToday] = [wnd.moment(), wnd.moment()];
        else if (preDefinedDateRangesName === 'rangeIntervalYesterday')
            ranges[rangeIntervalYesterday] = [wnd.moment().subtract(1, 'days'), wnd.moment().subtract(1, 'days')];
        else if (preDefinedDateRangesName === 'rangeLast7Days')
            ranges[rangeLast7Days] = [wnd.moment().subtract(6, 'days'), wnd.moment()];
        else if (preDefinedDateRangesName === 'rangeLast30Days')
            ranges[rangeLast30Days] = [wnd.moment().subtract(29, 'days'), wnd.moment()];
        else if (preDefinedDateRangesName === 'rangeThisMonth')
            ranges[rangeThisMonth] = [wnd.moment().startOf('month'), wnd.moment().endOf('month')];
        else if (preDefinedDateRangesName === 'rangeToMonthEnd')
            ranges[rangeToMonthEnd] = [wnd.moment(), wnd.moment().endOf('month')];
        else if (preDefinedDateRangesName === 'rangePreviousMonth')
            ranges[rangePreviousMonth] = [wnd.moment().subtract(1, 'month').startOf('month'), wnd.moment().subtract(1, 'month').endOf('month')];
        else if (preDefinedDateRangesName === 'rangeMonthStartToCurrentDate')
            ranges[rangeMonthStartToCurrentDate] = [wnd.moment().startOf('month'), wnd.moment()];
        else if (preDefinedDateRangesName === 'rangeThisYear')
            ranges[rangeThisYear] = [wnd.moment().startOf('year'), wnd.moment().endOf('year')];
        else if (preDefinedDateRangesName === 'rangeToYearEnd')
            ranges[rangeToYearEnd] = [wnd.moment(), wnd.moment().endOf('year')];
    }
    ranges[clear] = [null];
    return ranges;
}

var getSingleRanges = function (wnd, rangeToday, rangeYesterday, rangeSevenDaysAgo, rangeThirtyDaysAgo, rangeMonthStart, rangeMonthEnd,
                                rangePreviousMonthStart, rangePreviousMonthEnd, rangeThisYearStart, rangeThisYearEnd, clear, preDefinedDateRangesNames) {
    let ranges = {};
    for (let i = 0; i < preDefinedDateRangesNames.length; i++) {
        let preDefinedDateRangesName = preDefinedDateRangesNames[i];
        if (preDefinedDateRangesName === 'rangeToday')
            ranges[rangeToday] = [wnd.moment()];
        else if (preDefinedDateRangesName === 'rangeYesterday')
            ranges[rangeYesterday] = [wnd.moment().subtract(1, 'days')];
        else if (preDefinedDateRangesName === 'rangeSevenDaysAgo')
            ranges[rangeSevenDaysAgo] = [wnd.moment().subtract(7, 'days')];
        else if (preDefinedDateRangesName === 'rangeThirtyDaysAgo')
            ranges[rangeThirtyDaysAgo] = [wnd.moment().subtract(30, 'days')];
        else if (preDefinedDateRangesName === 'rangeMonthStart')
            ranges[rangeMonthStart] = [wnd.moment().startOf('month')];
        else if (preDefinedDateRangesName === 'rangeMonthEnd')
            ranges[rangeMonthEnd] = [wnd.moment().endOf('month')];
        else if (preDefinedDateRangesName === 'rangePreviousMonthStart')
            ranges[rangePreviousMonthStart] = [wnd.moment().subtract(1, 'month').startOf('month')];
        else if (preDefinedDateRangesName === 'rangePreviousMonthEnd')
            ranges[rangePreviousMonthEnd] = [wnd.moment().subtract(1, 'month').endOf('month')];
        else if (preDefinedDateRangesName === 'rangeThisYearStart')
            ranges[rangeThisYearStart] = [wnd.moment().startOf('year')];
        else if (preDefinedDateRangesName === 'rangeThisYearEnd')
            ranges[rangeThisYearEnd] = [wnd.moment().endOf('year')];
    }
    ranges[clear] = [null];
    return ranges;
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

function setColorTheme() {
    setCookie('LSFUSION_CLIENT_COLOR_THEME', window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches ? "dark" : "light");
    //window.matchMedia('(prefers-color-scheme: dark)').addEventListener('change', event => {
    //    //preferred color scheme changed
    //});
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
    window.onbeforeunload = null;
    document.location.reload();
}

function removeObjectFromArray(array, test) {
    let index = array.indexOf(array.find(obj => test(obj)));
    return array.slice(0, index).concat(array.slice(index + 1));
}

function replaceObjectFieldInArray(array, test, propertyName, newValue) {
    return array.map(oldObj => test(oldObj) ? replaceField(oldObj, propertyName, newValue) : oldObj);
}

function addObjectToArray(array, object, index) {
    return array.slice(0, index).concat(object).concat(array.slice(index));
}

function replaceOrAddObjectFieldInArray(array, test, propertyName, newValue, object) {
    //assume that only one object can be found
    let found = array.find(obj => test(obj));
    if (found)
        array[array.indexOf(found)] = replaceField(found, propertyName, newValue);
    else
        array.push(object);

    return array;
}

function replaceField(obj, propertyName, newValue) {
    return { ...obj, [propertyName] : newValue };
}

function plainEquals(object1, object2, ignoreField) {
    if(object1 === object2)
        return true;

    var object1Keys = Object.keys(object1);
    var object2Keys = Object.keys(object2);

    return !(object1Keys.length !== object2Keys.length || (object1Keys.find(function (object1Key) { return object1Key !== ignoreField && object1[object1Key] !== object2[object1Key]}) !== undefined));
}

function isContainHtmlTag(value) {
    return value.match(".*\\<[^>]+\\>(.|\n|\r)*");
}