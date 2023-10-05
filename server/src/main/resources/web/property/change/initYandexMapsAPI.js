function initYandexMapsAPI(key, commercial) {
    lsfParams.yandexMapAPI = L.yandex()
        .loadApi({
            apiParams: key,
            apiUrl: commercial != null ? 'https://enterprise.api-maps.yandex.ru/{version}/' : 'https://api-maps.yandex.ru/{version}/'
        });
}