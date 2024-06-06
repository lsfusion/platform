function createManifest(contextPath, manifestElement, name, icon) {
    let currentLocation = window.location.origin + contextPath + "/";
    let manifest = {
        "name": name,
        "icons": [
            {
                "src": currentLocation + icon,
                "type": "image/png",
                "sizes": "512x512"
            }
        ],
        "id": (contextPath ? contextPath + "/" : "") + "main",
        "start_url": currentLocation + "main",
        "display": "standalone",
        "scope": currentLocation,
    }

    let stringManifest = JSON.stringify(manifest);
    let blob = new Blob([stringManifest], {type: 'application/json'});
    let manifestURL = URL.createObjectURL(blob);
    manifestElement.setAttribute('href', manifestURL);
}