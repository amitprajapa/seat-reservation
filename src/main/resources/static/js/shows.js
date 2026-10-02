async function load() {
  let box = document.getElementById("shows");
  try {
    let x = Api.data(await Api.request("/api/shows"));
    let a = Array.isArray(x) ? x : x?.content || x?.shows || [];
    box.innerHTML = a.length
      ? a
          .map(
            (s) =>
              `<article class="card"><div class="tag">LIVE SHOW</div><h3>${esc(s.name || "Show " + s.id)}</h3><p class="muted">Show ID: ${s.id}</p><div class="cardfoot"><b>${Api.money(s.pricePaise)}</b><a class="btn" href="seat-selection.html?showId=${s.id}">Choose seats →</a></div></article>`,
          )
          .join("")
      : "No shows available.";
  } catch (e) {
    msg(e.message, "error");
  }
}
document.addEventListener("DOMContentLoaded", () => {
  load();
  document.getElementById("refresh")?.addEventListener("click", load);
});
