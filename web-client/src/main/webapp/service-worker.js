self.addEventListener('install', function(event) {
    // Perform any install steps, if needed
});

self.addEventListener('activate', function(event) {
    // Perform any necessary cleanup or other tasks
});

self.addEventListener('push', function(event) {
    let data = event.data.json();
    let showPushNotification = (action, push) => {
        if(data.notification)
            return showNotification(data.notification, action, data.inputActions, push);
        return Promise.resolve();
    }
    if(data.alwaysNotify)
        event.waitUntil(showPushNotification(data.action, data.push));
    else
        dispatchAction(event, { action: data.action, result: null }, data.push, (client) => showFocusNotification(client), (actionResult, push) => showPushNotification(actionResult.action, push));
})

self.addEventListener('notificationclick', function(event) {
    // Do something when notification is clicked
    event.notification.close();

    // https://developer.mozilla.org/en-US/docs/Web/API/NotificationEvent/notification
    // sent in options.data
    let data = event.notification.data;
    if(data.action.type === 'focusNotification')
        event.waitUntil(clients.get(data.action.clientId).then((client) => client.focus()));
    else
        dispatchAction(event, { action: data.action, result: event.action } , data.push, (client) => client.focus(),
            (actionResult, push) => clients.openWindow("main" + (push.query ? "?" + push.query : "")).then((client) => pushPendingNotification(client, actionResult)));
});

function showNotification(notification, action, inputActions, push) {
    // should have dispatchAction fields (id, url, query)
    return self.registration.showNotification(!notification.title?.length/*check that the string is not null or empty https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Operators/Optional_chaining*/
            ? defaultNotification.title : notification.title,
        addServiceWorkerData(defaultNotification.options, notification.options, {action: action, push: push}, inputActions));
}
function pushNotification(client, actionResult) {
    let action = actionResult.action;
    if(action.id)
        pushNotificationId(client, action.id, actionResult.result);
    else
        return fetch(action.url, {
            headers: {
                'Need-Notification-Id' : 'TRUE'
            }
        }).then(response => {
            return response.text();
        }).then(text => {
            pushNotificationId(client, parseInt(text), actionResult.result);
        });
}
function pushNotificationId(client, actionId, actionResult) {
    client.postMessage( { type: 'pushNotification', id: actionId, result: actionResult } );
}
const pendingNotifications = {};
function pushPendingNotification(client, actionResult) {
    if (client.id in pendingNotifications) { // assert equals "checked", so event listener is already installed, we can send notification
        pushNotification(client, actionResult);
        delete pendingNotifications[client.id]
    } else {
        pendingNotifications[client.id] = actionResult;
    }
}
function pullNotification(client) {
    if (client.id in pendingNotifications) {
        pushNotification(client, pendingNotifications[client.id]);
        delete pendingNotifications[client.id];
    } else {
        pendingNotifications[client.id] = 'checked';
    }
}
function showFocusNotification(client) {
    return showNotification(focusNotification, { type: 'focusNotification', clientId: client.id}, [], null);
}

let defaultNotification, focusNotification;
self.addEventListener('message', function (event) {
    // sent in postMessage param
    let data = event.data;
    let messageClient = event.source;
    if(data.type === 'pushNotification') { // message from push-notification.jsp
        dispatchAction(event, { action: {id: data.actionId}, result: null }, data.push, (client) => {
            messageClient.postMessage('close');
            return showFocusNotification(client);
        }, (actionResult, push) =>
            messageClient.navigate("main" + (push.query ? "?" + push.query : "")).then((client) => pushPendingNotification(client, actionResult))
        );
    } else if(data.type === 'pullNotification') { // message from the main frame (after message listener is added)
        messageClient.postMessage({type: 'clientId', clientId: messageClient.id});
        pullNotification(messageClient);
    } else if(data.type === 'showNotification') {
        showNotification(data.notification, data.action, data.inputActions, data.push);
    } else if (data.type === 'setDefaultNotifyOptions') {
        defaultNotification = data.defaultNotification;
        focusNotification = data.focusNotification;
    }
})

// action should have: id or (and) url, push should have - clientId (optional), query
function dispatchAction(event, actionResult, push, onClientFound, onClientNotFound) {
    event.waitUntil(
        clients
            .matchAll({
                type: 'window',
            })
            .then((clientList) => {
                let webClient = null;
                if(push.clientId) {
                    for (const client of clientList)
                        if (client.id === push.clientId)
                            webClient = client;
                }

                if(webClient == null) {
                    for (const client of clientList) {
                        // Get the full URL of the request
                        const url = new URL(client.url);

                        // Get the URL without the origin
                        // url pathname = "/" or /main/
                        if (url.search === push.query)
                            webClient = client;
                    }
                }

                if(webClient !== null) {
                    pushNotification(webClient, actionResult);
                    return onClientFound(webClient);
                } else
                    return onClientNotFound(actionResult, push);
            })
    );
}

function addServiceWorkerData (defaultOptions, options, newData, newActions) {
    return {
        ...defaultOptions,
        ...(options || {}),
        data: {
            ...(options?.data || {}),
            ...newData
        },
        actions: options?.actions || newActions
    };
}