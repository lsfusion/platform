
renderAutocompleteTextBased = function (element) {
    let autocompleteOptions = {
        types: ['address'],
        componentRestrictions: {
            country: "BLR"
        }
    };

    let input = document.createElement("input");
    input.setAttribute('type', 'text');
    element.appendChild(input);

    new google.maps.places.Autocomplete(input, autocompleteOptions);
}

clearRenderAutocompleteTextBased = function (element) {
    element.innerHtml = '';
    $(".pac-container").remove(); // remove autocomplete elements from <body>
}