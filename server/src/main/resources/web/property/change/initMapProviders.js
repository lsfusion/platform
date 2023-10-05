//load Google-api if it was not loaded earlier
//https://issuetracker.google.com/issues/35820648
function initGoogleAPI(key) {
    if (key != null && (typeof google !== 'object' || typeof google.maps !== 'object' || typeof google.maps.places !== 'object')) {
        $.getScript('https://maps.googleapis.com/maps/api/js?key=' + key + '&libraries=places&callback=Function.prototype');
        lsfParams.googleMapsAPILoaded = true;
    }
}

function initYandexAPI(key, commercial) {
    lsfParams.yandexMapAPI = L.yandex()
        .loadApi({
            apiParams: key,
            apiUrl: commercial != null ? 'https://enterprise.api-maps.yandex.ru/{version}/' : 'https://api-maps.yandex.ru/{version}/'
        });
}