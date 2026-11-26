(function () {
  var deck = document.querySelector('[data-deck]');
  if (!deck) return;
  var cards = deck.querySelectorAll('[data-card]');
  var nav = deck.querySelector('[data-nav]');
  var position = deck.querySelector('[data-position]');
  var prev = deck.querySelector('[data-prev]');
  var next = deck.querySelector('[data-next]');
  if (!cards.length || !nav || !position || !prev || !next) return;
  var current = 0;
  function show(index, focus) {
    current = Math.max(0, Math.min(cards.length - 1, index));
    for (var i = 0; i < cards.length; i++) cards[i].hidden = i !== current;
    position.textContent = (current + 1) + ' / ' + cards.length;
    prev.disabled = current === 0;
    next.disabled = current === cards.length - 1;
    if (focus) cards[current].focus();
  }
  prev.addEventListener('click', function () { show(current - 1, false); });
  next.addEventListener('click', function () { show(current + 1, false); });
  document.addEventListener('keydown', function (event) {
    if (event.altKey || event.ctrlKey || event.metaKey) return;
    if (event.key === 'ArrowLeft') show(current - 1, false);
    if (event.key === 'ArrowRight') show(current + 1, false);
  });
  var links = deck.querySelectorAll('[data-jump]');
  for (var j = 0; j < links.length; j++) {
    links[j].addEventListener('click', function (event) {
      event.preventDefault();
      show(Number(this.getAttribute('data-jump')), true);
    });
  }
  nav.hidden = false;
  deck.classList.add('is-paged');
  show(0, false);
})();
