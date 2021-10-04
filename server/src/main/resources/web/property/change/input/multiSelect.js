let selectInstance = null;
let selectElement = document.createElement('select');

function testMultiSelect(element, list, controller) {
    //if not exist - create
    if (!element.contains(selectElement)) {
        //remove old instance
        if (selectInstance != null) {
            selectInstance.select2('destroy');
            selectElement.innerHTML = '';
        }

        //create
        element.appendChild(selectElement);
        selectInstance = $(selectElement).select2({
            ajax: {
                transport: function (params, success, failure) {
                    let value = params.data.term;
                    controller.getValues('name', value == null ? '' : value, success, failure);
                },
                processResults: function (data, params) {
                    return {
                        results: mapProperties(data, controller)
                    };
                }
            },
            multiple: true,
            placeholder: 'This is placeholder', //todo
            closeOnSelect: false,
            data: mapProperties(list, controller),
            allowClear: true,
            width: 'auto' //todo
        });

        selectInstance.on('select2:select', function (e) {
            controller.changeProperty('selected', e.params.data.originalElement, true);
        });

        selectInstance.on('select2:unselect', function (e) {
            let data = e.params.data;
            controller.changeProperty('selected', data.originalElement == null ? data.element.originalElement : data.originalElement, null);
        });

    } else { // if exist - update
        let currentSelectedOptions = selectInstance.find(':selected');

        //cancel pressed or delete all items
        let selectContext = selectInstance.context;
        if (currentSelectedOptions.length === 0 && list.length > 0) {
            //remove all child elements
            while (selectContext.firstChild) {
                selectContext.removeChild(selectContext.lastChild);
            }
        }

        //look items that are in list and unselected and select them
        for (let listElement of list) {
            let selected = false;
            for (let i = 0; i < currentSelectedOptions.length; i++) {
                if (listElement['#__key'].toString() === currentSelectedOptions[i].value) {
                    selected = true;
                    currentSelectedOptions.splice(i, 1);
                    break;
                }
            }

            if (!selected) {
                // Append it to the select
                let option = new Option(listElement.name, listElement['#__key'].toString(), listElement.selected, listElement.selected);
                option.disabled = controller.isPropertyReadOnly('selected', listElement);
                option.originalElement = listElement['#__key'];
                selectInstance.append(option);
            }
        }

        // check if currentSelectedOptions not empty and remove elements
        for (let findElement of currentSelectedOptions) {
            findElement.selected = false;
            selectContext.removeChild(findElement);
            selectInstance.trigger('change');
        }
    }
}

function mapProperties(properties, controller) {
    let isListProperty = Array.isArray(properties) && properties.length > 0 && properties[0].key == null;

    return $.map(isListProperty ? properties : properties.data, function (obj) {
        obj.id = isListProperty ? obj['#__key'].toString() : obj.key.toString();
        obj.text = isListProperty ? obj.name : obj.rawString;
        obj.disabled = controller.isPropertyReadOnly('selected', isListProperty ? obj : obj.key);
        obj.originalElement = isListProperty ? obj['#__key'] : obj.key;
        return obj;
    });
}