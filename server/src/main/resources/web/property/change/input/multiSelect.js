function multiSelect(element, list, controller) {
    let selectInstance = $(element.firstChild);
    if (!selectInstance.length) {
        let selectElement = document.createElement('select');
        element.appendChild(selectElement);

        selectInstance = $(selectElement).select2({
            ajax: {
                transport: function (params, success, failure) {
                    let value = params.data.term;
                    controller.getValues('name', value == null ? '' : value, success, failure);
                },
                processResults: function (data, params) {
                    return {
                        results: $.map(data.data, function (obj) {
                            obj.id = obj.rawString;
                            obj.text = obj.rawString;
                            return obj;
                        })
                    };
                }
            },
            multiple: true,
            placeholder: 'This is placeholder', //todo
            closeOnSelect: false,
            allowClear: true,
            width: 'auto' //todo
        });

        selectInstance.on('select2:select', function (e) {
            controller.changeProperty('selected', e.params.data.key, true);
        });

        selectInstance.on('select2:unselect', function (e) {
            controller.changeProperty('selected', e.params.data.key, null);
        });
    }

    //look items that are in list and unselected and select them
    let optionsParent = selectInstance.context;
    let currentOptions = Array.from(optionsParent.children);

    for (let listElement of list) {
        let selected = false;
        for (let i = 0; i < currentOptions.length; i++) {
            if (currentOptions[i].key != null && currentOptions[i].key.name === listElement.name) {
                selected = true;
                currentOptions.splice(i, 1);
                break;
            }
        }

        if (!selected && listElement.selected === true) {
            // Append it to the select
            let option = new Option(listElement.name, listElement.name, listElement.selected, listElement.selected);
            option.key = listElement;
            selectInstance.append(option);

            selectInstance.select2('data').at(-1).key = listElement;
        }
    }

    // check if currentOptions not empty and remove elements
    currentOptions.forEach(option => optionsParent.removeChild(option));
}