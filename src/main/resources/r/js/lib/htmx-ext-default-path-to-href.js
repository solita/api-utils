htmx.defineExtension('default-path-to-href', {
    onEvent: function(name, evt) {
        if (name === "htmx:configRequest") {
            if (evt.detail.elt.hasAttribute('hx-get') && evt.detail.elt.getAttribute('hx-get') === "") {
                evt.detail.path = evt.detail.elt.getAttribute('href');
            }
        }
    }
});