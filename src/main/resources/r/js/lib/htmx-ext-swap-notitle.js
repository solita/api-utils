htmx.defineExtension('swap-notitle', {
    handleSwap: function(swapStyle, target, fragment, settleInfo) {
        if ((settleInfo.elts[0].getAttribute('hx-swap') || '').split(/\s+/).includes("notitle")) {
            delete settleInfo.title;
        }
        return false; // always return false to continue processing
    },
});