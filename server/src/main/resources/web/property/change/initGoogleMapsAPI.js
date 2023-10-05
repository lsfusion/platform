//load Google-api if it was not loaded earlier
//https://issuetracker.google.com/issues/35820648
function initGoogleMapsAPI(key) {
    if (key != null && (typeof google !== 'object' || typeof google.maps !== 'object' || typeof google.maps.places !== 'object')) {
        $.getScript('https://maps.googleapis.com/maps/api/js?key=' + key + '&libraries=places&callback=Function.prototype');
        lsfParams.googleMapsAPILoaded = true;
    }
}