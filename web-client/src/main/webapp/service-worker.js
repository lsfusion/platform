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
            return showNotification(data.notification, action, push);
        return Promise.resolve();
    }
    if(data.alwaysNotify)
        event.waitUntil(showPushNotification(data.action, data.push));
    else
        dispatchAction(event, data.action, data.push, (client) => showFocusNotification(client), showPushNotification);
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
        dispatchAction(event, data.action, data.push, (client) => client.focus(),
            (action, push) => clients.openWindow("/" + (push.query ? "?" + push.query : "")).then((client) => pushPendingNotification(client, action)));
});


function showNotification(notification, action, push) {
    // should have dispatchAction fields (id, url, query)
    return self.registration.showNotification(notification.title, addServiceWorkerData(notification.options, {action: action, push: push}));
}
function pushNotification(client, action) {
    if(action.id)
        pushNotificationId(client, action.id);
    else
        return fetch(action.url, {
            headers: {
                'Need-Notification-Id' : 'TRUE'
            }
        }).then(response => {
            return response.text();
        }).then(text => {
            pushNotificationId(client, parseInt(text))
        });
}
function pushNotificationId(client, actionId) {
    client.postMessage( { type: 'pushNotification', id: actionId } );
}
const pendingNotifications = {};
function pushPendingNotification(client, action) {
    if (client.id in pendingNotifications) { // assert equals "checked", so event listener is already installed, we can send notification
        pushNotification(client, action);
        delete pendingNotifications[client.id]
    } else {
        pendingNotifications[client.id] = action;
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
    return showNotification({ title: "New window opened", options: {}}, { type: 'focusNotification', clientId: client.id}, null);
}

self.addEventListener('message', function (event) {
    // sent in postMessage param
    let data = event.data;
    let messageClient = event.source;
    if(data.type === 'pushNotification') { // message from push-notification.jsp
        dispatchAction(event, {id: data.actionId}, data.push, (client) => {
            messageClient.postMessage('close');
            return showFocusNotification(client);
        }, (action, push) =>
            messageClient.navigate("/" + (push.query ? "?" + push.query : "")).then((client) => pushPendingNotification(client, action))
        );
    } else if(data.type === 'pullNotification') { // message from the main frame (after message listener is added)
        messageClient.postMessage({type: 'clientId', clientId: messageClient.id});
        pullNotification(messageClient);
    } else if(data.type === 'showNotification') {
        showNotification(data.notification, data.action, data.push);
    }
})

// action should have: id or (and) url, push should have - clientId (optional), query
function dispatchAction(event, action, push, onClientFound, onClientNotFound) {
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
                    pushNotification(webClient, action);
                    return onClientFound(webClient);
                } else
                    return onClientNotFound(action, push);
            })
    );
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