export function generateInjectedJs(selector: string, countSelector?: string): string {
  return `
(function() {
  function extract() {
    var products = [];
    var links = document.querySelectorAll('${selector}');
    for (var i = 0; i < links.length; i++) {
      var name = links[i].textContent.trim();
      var href = links[i].getAttribute('href') || '';
      if (name) products.push({ name: name, href: href });
    }
    ${
      countSelector
        ? `var countInput = document.querySelector('${countSelector}');
    var count = countInput ? parseInt(countInput.value, 10) : products.length;`
        : `var count = products.length;`
    }
    window.ReactNativeWebView.postMessage(JSON.stringify({ count: count, products: products }));
  }
  if (document.readyState === 'complete') {
    setTimeout(extract, 1000);
  } else {
    window.addEventListener('load', function() { setTimeout(extract, 1000); });
  }
})();
true;
`;
}
