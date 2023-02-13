tippy(document.body.querySelectorAll('[title]'), {
    allowHTML: true,
    flipOnUpdate: true,
    maxWidth: 'none',
    interactive: true,
    content(reference) {
        const title = reference.getAttribute('title');
        reference.removeAttribute('title');
        return title;
    },
    onMount(instance) {
        if (htmx) {
            htmx.process(instance.reference.parentElement);
        }
    }
});