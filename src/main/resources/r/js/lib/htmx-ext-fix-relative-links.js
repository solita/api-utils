htmx.defineExtension('fix-relative-links', {
    onEvent: function(name, evt) {
        let isRelativeLink = function(a) {
            return !/^https?:\/\//i.test(a.getAttribute('href'));
        }
        if (name === 'htmx:afterSwap') {
            evt.detail.target.querySelectorAll('a[href]').forEach(function(a) {
                if (isRelativeLink(a)) {
                    a.href = evt.detail.pathInfo.requestPath + (a.getAttribute('href').startsWith('/') ? '' : evt.detail.pathInfo.responsePath) + a.getAttribute('href');
                }
            })
        }
    }
});
