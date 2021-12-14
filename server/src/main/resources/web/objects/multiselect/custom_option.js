function option() {
    return {
        render: function (element, controller) {
            let options = document.createElement('options');
            options.style.display = 'block';
            options.style.borderRadius = 'var(--table-border-radius)';
            options.style.border = '1px solid var(--grid-separator-border-color)';
            options.style.padding = 'var(--border-padding) 4px';

            element.appendChild(options);
            element.options = options;
        },
        update: function (element, controller, list) {
            let options = element.options;
            let diff = controller.getDiff(list, true);

            diff.update.forEach(rawOption => Array.from(options.children).forEach(o => {
                if (controller.getKey(o.key).toString() === controller.getKey(rawOption).toString()) {
                    o.innerText = rawOption.name;
                    o.selected = rawOption.selected;
                    o.index = rawOption.index;
                    o.style.backgroundColor = rawOption.selected ? 'var(--selection-color)' : 'unset';
                }
            }));

            diff.remove.forEach(rawOption => {
                let key = controller.getKey(rawOption).toString();
                Array.from(options.children).forEach(o => {
                    if (controller.getKey(o.key).toString() === key)
                        options.removeChild(o);
                });
            });

            diff.add.forEach(rawOption => {
                let option = document.createElement('div');
                option.innerText = rawOption.name;
                option.key = rawOption;
                option.selected = rawOption.selected;
                option.index = rawOption.index;

                option.style.backgroundColor = rawOption.selected ? 'var(--selection-color)' : 'unset';
                option.style.display = 'inline-block';
                option.style.padding = '5px';
                option.style.border = 'var(--border-width) solid var(--component-border-color)';
                option.style.borderRadius = 'var(--table-border-radius)';
                option.style.margin = '3px 3px 3px 0';

                option.addEventListener('mouseover', function () {
                    this.style.backgroundColor = 'var(--component-hover-background-color)';
                    this.style.cursor = 'pointer';
                });

                option.addEventListener('mouseout', function () {
                    this.style.backgroundColor = this.selected ? 'var(--selection-color)' : 'unset';
                });

                option.addEventListener('click', function () {
                    controller.changeProperty('selected', this.key, !this.selected ? true : null);
                    controller.changeSimpleGroupObject(this.key, false, null);
                });

                let currentOptions = Array.from(options.children);
                if (option.index === (currentOptions.length))
                    options.appendChild(option);
                else
                    options.insertBefore(option, currentOptions[option.index]);
            });

            //set Current
            Array.from(options.children).forEach(o =>
                o.style.border = 'var(--border-width) solid var(' + (controller.isCurrent(o.key) ? '--focused-cell-border-color)' : '--component-border-color)'));
        }
    }
}