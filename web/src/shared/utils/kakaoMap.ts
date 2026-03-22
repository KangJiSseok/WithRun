export function createMarkerContent(label: string, color: string): HTMLElement {
  const wrapper = document.createElement('div');
  wrapper.style.cssText =
    'display:flex;flex-direction:column;align-items:center;gap:4px;pointer-events:none;user-select:none;';
  wrapper.draggable = false;

  const dot = document.createElement('div');
  dot.style.cssText = `
    width: 20px;
    height: 20px;
    background: ${color};
    border: 3px solid white;
    border-radius: 50%;
    box-shadow: 0 2px 6px rgba(0,0,0,0.3);
  `;

  const tag = document.createElement('span');
  tag.textContent = label;
  tag.style.cssText = `
    background: ${color};
    color: white;
    font-size: 11px;
    font-weight: 700;
    padding: 2px 6px;
    border-radius: 4px;
    white-space: nowrap;
    box-shadow: 0 1px 3px rgba(0,0,0,0.2);
  `;

  wrapper.appendChild(dot);
  wrapper.appendChild(tag);
  return wrapper;
}
