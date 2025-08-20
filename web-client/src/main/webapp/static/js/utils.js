
var createPlainObject = function () {
    return {};
};

function createPlainDateMillis(millis) {
    return new Date(millis);
}
function createPlainDateCurrent() {
    return new Date();
}
function createPlainDate(year, month, date) {
    return new Date(year, month, date);
}
function createPlainDateTime(year, month, date, hours, minutes, seconds, milliseconds) {
    return new Date(year, month, date, hours, minutes, seconds, milliseconds);
}
function createPlainDateTimeUTC(year, month, date, hours, minutes, seconds) {
    return new Date(Date.UTC(year, month, date, hours, minutes, seconds));
}

function getClientDateTimeFormat() {
    return Intl.DateTimeFormat().resolvedOptions();
}

//this var is needed to localize daterangepicker, because GWT does not accept dynamic keys in arrays
var getRanges = function (wnd, rangeIntervalToday, rangeIntervalYesterday, rangeLast7Days, rangeLast30Days, rangeThisMonth, rangeToMonthEnd,
                          rangePreviousMonth, rangeMonthStartToCurrentDate, rangeThisYear, rangeToYearEnd, clear, preDefinedDateRangesNames) {
    let ranges = {};
    for (let i = 0; i < preDefinedDateRangesNames.length; i++) {
        let preDefinedDateRangesName = preDefinedDateRangesNames[i];
        if (preDefinedDateRangesName === 'rangeIntervalToday')
            ranges[rangeIntervalToday] = [wnd.moment().startOf('day'), wnd.moment().endOf('day')];
        else if (preDefinedDateRangesName === 'rangeIntervalYesterday')
            ranges[rangeIntervalYesterday] = [wnd.moment().subtract(1, 'days').startOf('day'), wnd.moment().subtract(1, 'days').endOf('day')];
        else if (preDefinedDateRangesName === 'rangeLast7Days')
            ranges[rangeLast7Days] = [wnd.moment().subtract(6, 'days').startOf('day'), wnd.moment().endOf('day')];
        else if (preDefinedDateRangesName === 'rangeLast30Days')
            ranges[rangeLast30Days] = [wnd.moment().subtract(29, 'days').startOf('day'), wnd.moment().endOf('day')];
        else if (preDefinedDateRangesName === 'rangeThisMonth')
            ranges[rangeThisMonth] = [wnd.moment().startOf('month'), wnd.moment().endOf('month')];
        else if (preDefinedDateRangesName === 'rangeToMonthEnd')
            ranges[rangeToMonthEnd] = [wnd.moment(), wnd.moment().endOf('month')];
        else if (preDefinedDateRangesName === 'rangePreviousMonth')
            ranges[rangePreviousMonth] = [wnd.moment().subtract(1, 'month').startOf('month'), wnd.moment().subtract(1, 'month').endOf('month')];
        else if (preDefinedDateRangesName === 'rangeMonthStartToCurrentDate')
            ranges[rangeMonthStartToCurrentDate] = [wnd.moment().startOf('month'), wnd.moment().endOf('day')];
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
var lsfFiles = window.lsfFiles || {};

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

function jsDateEquals(date1, date2) {
    if (date1 === date2)
        return true;
    if (!date1 || !date2)
        return false;

    return date1.getTime() === date2.getTime();
}

function containsLineBreak(value) {
    return value.indexOf("\n") >= 0;
}
function containsHtmlTag(value) {
    return value != null && value.match(".*\\<[^>]+\\>(.|\n|\r)*");
}

// actually it is also data, however usually it's metadata
function initCaptionHtmlOrText(element, renderer, hasCapitalHyphensProblem) {
    element.hasCapitalHyphensProblem = hasCapitalHyphensProblem;
    initHtmlOrText(element, renderer);
}
function initDataHtmlOrText(element, renderer) {
    initHtmlOrText(element, renderer);
}
function clearDataHtmlOrText(element, renderer) {
    clearHtmlOrText(element, renderer);
}

const englishVowels = ['a', 'e', 'i', 'o', 'u'];

function getFirstWord(text) {
    const length = text.length;
    let i = 0;

    // Iterate through the string to find the first non-letter character
    while (i < length && ((text[i] >= 'A' && text[i] <= 'Z') || (text[i] >= 'a' && text[i] <= 'z'))) {
        i++;
    }

    // Return the substring from the start to the first non-letter character
    return text.slice(0, i);
}

// the problem that some languages doesnot hyphenate capital words (because consider them "names"), but all captions usually starts with capitals so we have to hyphenate first word manually
function fixHyphens(element, value) {
    if (element.hasCapitalHyphensProblem && value != null && value.length >= 6) {
        let firstLetter = value.charAt(0);
        if (firstLetter === firstLetter.toUpperCase() && firstLetter !== firstLetter.toLowerCase()) {
            let firstWord = getFirstWord(value);
            if(firstWord.length >= 6) {
                let transformedValue = firstWord.slice(0, 3);
                let i = 3; // we don't want split when there are less or equal than 2 letters in the beginning
                let nextChar;
                // looking for not vowel
                while (i < firstWord.length && englishVowels.includes(nextChar = firstWord.charAt(i))) {
                    transformedValue += nextChar;
                    i++;
                }
                let nonVowelChar = nextChar; // next is non-vowel
                // looking for vowel
                while (i + 1 < firstWord.length && !englishVowels.includes(nextChar = firstWord.charAt(i + 1))) {
                    transformedValue += nonVowelChar;
                    i++;
                    nonVowelChar = nextChar;
                }
                if (i + 2 < firstWord.length) {  // we don't want split when there are less or equal than 2 letters in the end
                    // let vowelChar = nextChar; // next is vowel-char, but we just slice form the nonVowel
                    // if (value !== transformedValue + value.slice(i))
                    //     throw new Error("bug");
                    return transformedValue + '\u00AD' + value.slice(i);
                }
            }
        }
    }
    return value;
}
function setCaptionHtmlOrText(element, value) {
    let html = containsHtmlTag(value);
    if(!html)
        value = fixHyphens(element, value);

    setHtmlOrText(element, value, html);
}
function setCaptionNodeText(node, value) {
    setNodeText(node, fixHyphens(node.parentElement, value));
}
function setDataHtmlOrText(element, value, html) {
    setHtmlOrText(element, value, html)
}

function initHtmlOrText(element, renderer) {
    renderer(element, true);
}
function clearHtmlOrText(element, renderer) {
    renderer(element, false);

    element.classList.remove("html-or-text-no-multi-line");

    element.classList.remove("html-or-text-is-html");
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
        element.setAttribute('onmousedown', 'event.preventDefault()');
    else
        element.removeAttribute('onmousedown')
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
    try {
        navigator.serviceWorker.addEventListener("message", (event) => {
            onMessage(event.data);
        });
        navigator.serviceWorker.register('service-worker.js');
        postServiceWorkerMessage(message);
    } catch (error) {
        console.warn(error)
    }
}

function subscribePushManager(publicKey, onSubscribe) {
    try {
        navigator.serviceWorker.ready.then(function (registration) {
            registration.pushManager.getSubscription().then((subscription) => {
                if (!subscription) {
                    registration.pushManager.subscribe({
                        userVisibleOnly: true,
                        applicationServerKey: base64UrlToUint8Array(publicKey)
                    }).then((subscription) => {
                        onSubscribe(JSON.stringify(subscription));
                    });
                } else {
                    onSubscribe(JSON.stringify(subscription));
                }
            })
        });
    } catch (error) {
        console.warn(error);
    }
}

// IF THE KEY IS CHANGED, IT WILL BE NECESSARY TO UNSUBSCRIBE THE USER FROM NOTIFICATIONS, AND THEN SIGN AGAIN WITH THE NEW KEY, OTHERWISE ERRORS WILL OCCUR!
function unsubscribePushManager() {
    try {
        navigator.serviceWorker.ready.then(function (registration) {
            registration.pushManager.getSubscription().then((subscription) => {
                subscription.unsubscribe().then(()=> console.warn("unsubscribed"))
            })
        });
    } catch (error) {
        console.warn(error);
    }
}

function base64UrlToUint8Array(base64UrlData) {
    const padding = '='.repeat((4 - base64UrlData.length % 4) % 4);
    const base64 = (base64UrlData + padding)
        .replace(/\-/g, '+')
        .replace(/_/g, '/');

    const rawData = atob(base64);
    const buffer = new Uint8Array(rawData.length);

    for (let i = 0; i < rawData.length; ++i) {
        buffer[i] = rawData.charCodeAt(i);
    }

    return buffer;
}

function postServiceWorkerMessage(message) {
    try {
        return navigator.serviceWorker.ready.then((registration) => {
            registration.active.postMessage(message);
        });
    } catch (error) {
        console.warn(error);
    }
}

function webShare(shareData) {
    try {
        if(navigator.canShare(shareData)) {
            navigator.share(shareData);
            return true;
        }
    } catch (error) {
        console.warn(error);
    }
    return null;
}

// should correspond service worker's showFocusNotification
function showFocusNotification(message, caption, ifNotFocused) {
    webNotify({ title: caption, options: { body: message }, ifNotFocused: ifNotFocused}, { type: "focusNotification" }, [], null);
}

function webNotify(notification, action, inputActions, push) {
    return postServiceWorkerMessage({type: 'showNotification', notification: notification, action: action, inputActions: inputActions, push: push});
}

function requestPushNotificationPermissions() {
    try {
        Notification.requestPermission();
    } catch (error) {
        console.warn(error)
    }
}


function setAttributeOrStyle(element, attribute, value) {
    if (attribute in element.style)
        element.style.setProperty(attribute, value);
    else
        element.setAttribute(attribute, value)

}
function removeAttributeOrStyle(element, attribute, value) {
    if (attribute in element.style)
        element.style.removeProperty(attribute, value);
    else
        element.removeAttribute(attribute, value)
}

// Function is used in NativeHashMap.jsRemove()
// Because gwt throws an error when using js Map's .delete function, we have to use this hack
function jsMapDelete(map, key) {
    return map.delete(key);
}

function setGlobalClassName(set, className) {
    let root = document.documentElement;

    if (set)
        root.classList.add(className);
    else
        root.classList.remove(className);
}

// input with drop down
function handleInputKeyEvent(isOpen, controller, e, keyDown) {
    if(isOpen) { // is editing
        if(controller.isEditInputKeyEvent(e, true) || (keyDown && (e.key === 'Enter' || e.key === 'Escape')))
            e.stopPropagation()
    } else {
        if(controller.isRenderInputKeyEvent(e, true))
            e.stopPropagation();
    }
}
function handleDropdownKeyEvent(isOpen, e, keyDown) {
    if(isOpen) { // is editing
        if(keyDown && (e.keyCode === 38 || e.keyCode === 40 || e.key === 'Enter' || e.key === 'Escape'))
            e.stopPropagation()
    }
    if(e.keyCode === 32)
        e.stopPropagation();
}

function handleOptionKeyEvent(isButton, e, keyDown, isInGrid) {
    if (keyDown && e.shiftKey && isInGrid && ((isButton && (e.keyCode === 39 || e.keyCode === 37)) || (e.keyCode === 40 || e.keyCode === 38)))
        e.stopPropagation();
}

// is necessary to allow collapsible-text to be shown in exel theme
function addShowCollapsedContainerEvent(parent, toggleElementSelector, containerElementSelector, collapsibleClass) {
    let element = $(parent).find(toggleElementSelector);
    if (element.length > 0) {
        element.css('cursor', 'pointer');
        element.css('text-decoration', 'underline');
        element.on('click', function () {
            $(parent).find(containerElementSelector).each(function () {
                $(this).toggleClass(collapsibleClass);
            })
        })
    }
}

function mergeObjects(defaultObj, obj) {
        return {...defaultObj, ...obj};
}

function deepEquals(obj1, obj2) {
    if (obj1 === obj2)
        return true;

    if (typeof obj1 !== "object" || typeof obj2 !== "object" || obj1 === null || obj2 === null)
        return false;

    const keys1 = Object.keys(obj1);
    const keys2 = Object.keys(obj2);

    if (keys1.length !== keys2.length)
        return false;

    for (let key of keys1) {
        if (!keys2.includes(key) || !deepEquals(obj1[key], obj2[key]))
            return false;
    }

    return true;
}
