/**
 * Extension to fix loaded relative links to be relative
 * to the source page instead of the current one.
 */
htmx.defineExtension('fix-relative-links', {
    onEvent: function(name, evt) {
        let isRelative = function(href) {
            return !/^https?:\/\//i.test(href);
        }
        
        if (name === 'htmx:afterSwap') {
            evt.detail.target.querySelectorAll('a[href]').forEach(function(a) {
                let href = a.getAttribute('href');
                if (isRelative(href)) {
                    a.href = evt.detail.pathInfo.requestPath + (href.startsWith('/') ? '' : evt.detail.pathInfo.responsePath) + href;
                }
            })
        }
    }
});
