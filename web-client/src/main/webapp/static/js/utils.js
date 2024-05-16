
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

function containsLineBreak(value) {
    return value.indexOf("\n") >= 0;
}
function containsHtmlTag(value) {
    return value != null && value.match(".*\\<[^>]+\\>(.|\n|\r)*");
}

// actually it is also data, however usually it's metadata
function initCaptionHtmlOrText(element, cssClass) {
    initHtmlOrText(element, cssClass);
}
function initDataHtmlOrText(element, cssClass) {
    initHtmlOrText(element, cssClass);
}
function clearDataHtmlOrText(element, cssClass) {
    clearHtmlOrText(element, cssClass);
}
function setCaptionHtmlOrText(element, value) {
    setHtmlOrText(element, value, containsHtmlTag(value));
}
function setCaptionNodeText(node, value) {
    setNodeText(node, value);
}
function setDataHtmlOrText(element, value, html) {
    setHtmlOrText(element, value, html)
}

function initHtmlOrText(element, cssClass) {
    element.classList.add(cssClass);
}
function clearHtmlOrText(element, cssClass) {
    element.classList.remove("html-or-text-no-multi-line");

    element.classList.remove("html-or-text-is-html");

    element.classList.remove(cssClass);
}

function setHtmlOrText(element, value, html) {
    if(!html && (value == null || value.indexOf("\n") === -1)) // optimization
        element.classList.add("html-or-text-no-multi-line");
    else
        element.classList.remove("html-or-text-no-multi-line");

    if(html)
        element.classList.add("html-or-text-is-html");
    else
        element.classList.remove("html-or-text-is-html");

    if(html)
        element.innerHTML = value;
    else
        element.textContent = value; // will add linebreaks (maybe optimization of using textContent / nodeValue is possible)
}
function setNodeText(node, value) {
    node.nodeValue = value;
}

function setReadonlyNative(element, readonly) {
    element.readOnly = readonly;
}

// we don't want elements to receive focus by default
function createFocusElement(tag) {
    let element = document.createElement(tag);
    element.tabIndex = -1;
    return element;
}

function setReadonlyClass(element, readonly) {
    if(readonly)
        element.classList.add("is-readonly");
    else
        element.classList.remove("is-readonly");
}

function setReadonlyHeur(element, readonly) {
    if(readonly)
        element.setAttribute('onclick', 'return false');
    else
        element.removeAttribute('onclick')
}

function setDisabledNative(element, disabled) {
    element.disabled = disabled;
}

function setDisabledClass(element, readonly) {
    if(readonly)
        element.classList.add("is-disabled");
    else
        element.classList.remove("is-disabled");
}

function setReadonlyType(element, readonly) {
    if(element.tagName.toLowerCase() === 'input')
        setReadonlyNative(element, readonly);
    else
        setReadonlyClass(element, readonly);
}

function setDisabledType(element, readonly) {
    if(['button', 'fieldset', 'input', 'optgroup', 'option', 'option', 'textarea'].includes(element.tagName.toLowerCase()))
        setDisabledNative(element, readonly);
    else
        setDisabledClass(element, readonly);
}

function createFocusElementType(tag) {
    if(['a', 'area', 'button', 'iframe', 'input', 'object', 'select', 'textarea', 'summary'].includes(tag.toLowerCase()))
        return createFocusElement(tag);
    else
        return document.createElement(tag);
}

function openBroadcastChannel(channelName, handler) {
    let broadcastChannel = new BroadcastChannel(channelName);
    broadcastChannel.addEventListener("message", function(event) {
        handler(broadcastChannel, event.data);
    });
}

function postBroadcastChannelMessage(channel, message) {
    channel.postMessage(message);
}

function registerServiceWorker(onMessage, message) {
    navigator.serviceWorker.addEventListener("message", (event) => {
        onMessage(event.data);
    });
    navigator.serviceWorker.register('service-worker.js');
    postServiceWorkerMessage(message);
}

function postServiceWorkerMessage(message) {
    return navigator.serviceWorker.ready.then((registration) => {
        registration.active.postMessage(message);
    });
}

function addServiceWorkerData (options, newData) {
    return {
        ...(options || {}),
        data: {
            ...(options?.data || {}),
            ...newData
        }
    };
}

function webShare(shareData) {
    try {
        if(navigator.canShare(shareData)) {
            navigator.share(shareData);
            return true;
        }
    } catch (error) {
    }
    return null;
}

function webNotify(title, data, options) {
    // should have dispatchAction fields (notificationId, url, redirectURL)
    options = addServiceWorkerData(options, data);

    return postServiceWorkerMessage({type: 'showNotification', title: title, options: options});
}
