htmx.defineExtension('swap-notitle', {
    handleSwap: function(swapStyle, target, fragment, settleInfo) {
        delete settleInfo.title;
        return false; // always return false to continue processing
    }
});