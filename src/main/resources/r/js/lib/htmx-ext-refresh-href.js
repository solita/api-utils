/**
 * Extension that refreshes href attributes of anchor elements
 * based on actual URL used to make the Ajax call.
 * Refresh is made by default on "init" (initialization) and "mouseover" event.
 * Events can be changed by listing them in refresh-href -attribute, separated by comma.
 */
htmx.defineExtension('refresh-href', {
    onEvent: function(name, evt) {
        let tag = '_refresh_href_';
        
        if (name === "htmx:beforeRequest") {
            if (evt.detail.requestConfig.triggeringEvent === tag) {
                // event was sent by this extension -> call handler
                evt.detail.etc.handler(evt.detail.pathInfo.anchor, evt.detail.pathInfo.finalRequestPath);
                return false; // prevent actual Ajax call execution
            }
        } else if (name === "htmx:afterProcessNode") {
            let elt = evt.detail.elt;
            if (elt.tagName === "A" && elt.getAttribute('hx-get') && !elt.getAttribute("href")) {
                // element is an anchor with a hx-get attribute and with an empty/no href attribute.
                
                // events to hook this extension on.
                // act directly on initialization and mouseover by default.
                let node = htmx.closest(elt, "[refresh-href]");
                let events = (node && node.getAttribute('refresh-href') || "init,mouseover").split(",");
                
                events.forEach(function(e) {
                    let handler = function() {
                        // trigger an ajax call, but capture the url and interrupt request in htmx:beforeRequest
                        htmx.ajax("GET", elt.getAttribute('hx-get'), {
                            source: elt,
                            event: tag,
                            handler: function(anchor, finalRequestPath) {
                                let href = finalRequestPath + (anchor ? '#' + anchor : '');
                                elt.setAttribute('href', href);
                            }
                        });
                    };
                    if (e === 'init') {
                        handler();
                    } else {
                        elt.addEventListener(e, handler);
                    }
                });
            }
        }
    }
});