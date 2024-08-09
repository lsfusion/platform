function googleAutocomplete() {
    return customGoogleAutocomplete();
}

// backward compatibility
function customGoogleAutocomplete() {
    return {
        renderInput: (element, controller) => {
            if (lsfParams.googleMapsAPILoaded) {
                let autocompleteOptions = {
                    types: ['address'],
                    componentRestrictions: {
                        country: lsfParams.googleMapAutocompleteCountry
                    }
                };

                // it's tricky here, we don't use onSelectedEvent, but use side effect that autocomplete is rendered outside element
                // so blur happens on that selection event, which eventually leads to commit. But blur happens before value is set, so we defer onBlur
                controller.setDeferredCommitOnBlur(true);

                new google.maps.places.Autocomplete(element, autocompleteOptions);
            } else {
                let tooltipElement = document.createElement('tooltip')
                tooltipElement.style.setProperty("position", "fixed");
                tooltipElement.style.setProperty("color", "red");
                tooltipElement.style.setProperty("font-weight", "bold");
                tooltipElement.style.zIndex = 100000;

                let firstLineText = document.createTextNode("Google API key does not set");
                let secondLineText = document.createTextNode("Autocomplete is not available");
                let thirdLineText = document.createTextNode("Contact your administrator");
                tooltipElement.appendChild(firstLineText);
                tooltipElement.appendChild(document.createElement("br"));
                tooltipElement.appendChild(secondLineText);
                tooltipElement.appendChild(document.createElement("br"));
                tooltipElement.appendChild(thirdLineText);

                element.onmouseover = function (event) {
                    removeTooltipElement(tooltipElement);

                    tooltipElement.style.top = (event.pageY + 10) + 'px';
                    tooltipElement.style.left = (event.pageX + 10) + 'px';
                    document.body.appendChild(tooltipElement);
                }

                element.onmouseout = function () {
                    removeTooltipElement(tooltipElement);
                };

                element.onkeypress = function () {
                    removeTooltipElement(tooltipElement);
                };
            }
        },
        clear: (element, cancel) => {
            // remove autocomplete elements from <body>. https://stackoverflow.com/questions/33049322/no-way-to-remove-google-places-autocomplete
            $(".pac-container").remove();
        }
    };
}

function removeTooltipElement(element) {
    if (document.contains(element))
        document.getElementsByTagName('tooltip')[0].remove();// Remove last tooltip
}