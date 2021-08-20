let mapApiKeyGoogle = lsfParams.mapApiKey_Google;

if (mapApiKeyGoogle != null)
    $.getScript('https://maps.googleapis.com/maps/api/js?key=' + mapApiKeyGoogle + '&libraries=places');
else
    console.error("google key does not exist");

function customGoogleAutocomplete() {
    if (mapApiKeyGoogle == null)
        window.alert("google key does not exist");
    return {
        render: (element, editor) => {
            let autocompleteOptions = {
                types: ['address'],
                componentRestrictions: {
                country: lsfParams.googleMapAutocompleteCountry
                }
            };

            editor.setDeferredCommitOnBlur(true);

            new google.maps.places.Autocomplete(element, autocompleteOptions);
        },
        clear : (element) => {
            // remove autocomplete elements from <body>. https://stackoverflow.com/questions/33049322/no-way-to-remove-google-places-autocomplete
            $(".pac-container").remove();
        }
    };
}