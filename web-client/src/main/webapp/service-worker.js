self.addEventListener('install', function(event) {
    // Perform any install steps, if needed
});

self.addEventListener('activate', function(event) {
    // Perform any necessary cleanup or other tasks
});

self.addEventListener('push', function(event) {
    // const title = 'Notification Title';
    // const options = {
    //     body: 'Notification Body',
    //     icon: 'path/to/icon.png',
    //     badge: 'path/to/badge.png'
    // };
    //
    // event.waitUntil(self.registration.showNotification(title, options));

    // https://developer.mozilla.org/en-US/docs/Web/API/ServiceWorkerGlobalScope/push_event
    let data = event.data.json();
    if(data.notify)
        event.waitUntil(showNotification(data.title, data.options));
    else
        dispatchAction(event, data);
})

self.addEventListener('notificationclick', function(event) {
    // Do something when notification is clicked
    event.notification.close();

    // https://developer.mozilla.org/en-US/docs/Web/API/NotificationEvent/notification
    // sent in options.data
    let data = event.notification.data;
    if(data.type === 'focusNotification')
        event.waitUntil(clients.get(data.clientId).then((client) => focusWithNotification(client)));
    else
        dispatchAction(event, data);
});

function showNotification(title, options) {
    // return Notification.requestPermission().then((result) => {
    //     if (result === "granted") {
            return self.registration.showNotification(title, options);
        // }
    // });
}
function pushNotification(client, notificationId) {
    client.postMessage( { type: 'pushNotification', notificationId: notificationId } );
}
function focusWithNotification(client) {
    return client.focus().catch((error) => {
        return showNotification("New window opened", { data: { type: 'focusNotification', clientId: client.id} } );
    });
}

const pendingNotificationIds = {};

self.addEventListener('message', function (event) {
    // sent in postMessage param
    let data = event.data;
    let client = event.source;
    if(data.type === 'pushNotification') { // message from push-notification.jsp
        dispatchNotification(event, data, client);
    } else if(data.type === 'pullNotification') { // message from the main fram (after message listener is added)
        if(client.id in pendingNotificationIds) {
            pushNotification(client, pendingNotificationIds[client.id]);
            delete pendingNotificationIds[client.id];
        } else {
            pendingNotificationIds[client.id] = 'checked';
        }
    } else if(data.type === 'showNotification') {
        showNotification(data.title, data.options);
    } else if(data.type === 'getClientId') {
        client.postMessage({type: 'getClientId', clientId: client.id});
    }
})

// data should have: notificationId or (and) url, redirectUrl or (and) clientId
function dispatchAction(event, data) {
    // if(data.notificationId) {
        dispatchNotification(event, data, null);
    // } else { // fetching url, getting notificationId and sending in
    //
    // }
}

// data should have - notificationId, redirectUrl or clientId
function dispatchNotification(event, data, notificationClient) {
    event.waitUntil(
        clients
            .matchAll({
                type: 'window',
            })
            .then((clientList) => {
                let webClient = null;
                if(data.clientId) {
                    for (const client of clientList)
                        if (client.id === data.clientId)
                            webClient = client;
                }

                if(webClient == null) {
                    for (const client of clientList) {
                        // Get the full URL of the request
                        const url = new URL(client.url);

                        // Get the URL without the origin
                        if (url.pathname + url.search === data.redirectUrl)
                            webClient = client;
                    }
                }

                let notificationId = data.notificationId;

                if(webClient !== null) {
                    pushNotification(webClient, notificationId);

                    if(notificationClient != null)
                        notificationClient.postMessage('close');

                    return focusWithNotification(webClient);
                } else {
                    let redirectUrl = data.redirectUrl;
                    let ready;
                    if(notificationClient != null)
                        ready = notificationClient.navigate(redirectUrl);
                    else
                        ready = clients.openWindow(redirectUrl);
                    return ready.then((client) => {
                        if(client.id in pendingNotificationIds) { // assert equals "checked", so event listener is already installed, we can send notification
                            pushNotification(client, notificationId);
                            delete pendingNotificationIds[client.id]
                        } else {
                            pendingNotificationIds[client.id] = notificationId;
                        }
                    });
                }
            })
    );
}