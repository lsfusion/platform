$.getScript('');

createAutocompleteTextBased = function (element, editor) {
    let autocompleteOptions = {
        types: ['address'],
        componentRestrictions: {
            country: "BLR"
        }
    };


    editor.setDeferredCommitOnBlur(true);

    new google.maps.places.Autocomplete(element, autocompleteOptions);
}

clearRenderAutocompleteTextBased = function (element) {
    // remove autocomplete elements from <body>. https://stackoverflow.com/questions/33049322/no-way-to-remove-google-places-autocomplete
    $(".pac-container").remove();
}