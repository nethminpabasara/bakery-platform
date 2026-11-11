// Missing photos stay replaceable at their original paths. This is a neutral fallback.
document.addEventListener('error', (event) => {
  const image = event.target;
  if (!(image instanceof HTMLImageElement) || image.src.endsWith('/images/placeholder.svg')) return;
  image.dataset.originalSrc = image.getAttribute('src') || '';
  image.src = '/images/placeholder.svg';
}, true);
