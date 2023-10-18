class RotateLeftControl extends ol.control.Control {
    constructor(opt_options) {
      const options = opt_options || {};
      const button = document.createElement('button');
      button.innerHTML = '⟲';

      const element = document.createElement('div');
      element.className = 'rotate-left ol-unselectable ol-control';
      element.setAttribute("title", "Vastapäivään 45°. Alt+shift+drag pyörittää vapaasti.");
      element.appendChild(button);

      super({
        element: element,
        target: options.target,
      });

      button.addEventListener('click', () => {
        let view = this.getMap().getView();
        view.animate({
            duration: 250,
            rotation: view.getRotation() - Math.PI / 4,
        });
      }, false);
    }
}

class RotateRightControl extends ol.control.Control {
    constructor(opt_options) {
      const options = opt_options || {};
      const button = document.createElement('button');
      button.innerHTML = '⟳';

      const element = document.createElement('div');
      element.className = 'rotate-right ol-unselectable ol-control';
      element.setAttribute("title", "Myötäpäivään 45°. Alt+shift+drag pyörittää vapaasti.");
      element.appendChild(button);

      super({
        element: element,
        target: options.target,
      });

      button.addEventListener('click', () => {
        let view = this.getMap().getView();
        view.animate({
            duration: 250,
            rotation: view.getRotation() + Math.PI / 4,
        });
      }, false);
    }
}
