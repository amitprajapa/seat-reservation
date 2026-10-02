let sid = new URLSearchParams(location.search).get("showId"),
  price = 0,
  chosen = new Set(),
  seats = [];
document.addEventListener("DOMContentLoaded", async () => {
  if (!auth() || !sid) return;
  try {
    let s = Api.data(await Api.request("/api/shows/" + sid));
    document.getElementById("showTitle").textContent = s.name || "Show " + sid;
    document.getElementById("showMeta").textContent = "Show ID " + sid;
    price = Number(s.pricePaise || 0);
    document.getElementById("price").textContent = Api.money(price) + " / seat";
    await refresh();
  } catch (e) {
    msg(e.message, "error");
  }
  document.getElementById("reserve").addEventListener("click", reserve);
});
async function refresh() {
  let x = Api.data(await Api.request(`/api/shows/${sid}/seats`));
  seats = Array.isArray(x) ? x : x?.seats || [];
  let box = document.getElementById("seats");
  box.innerHTML = seats
    .map((s) => {
      let busy = String(s.status).toUpperCase() !== "AVAILABLE";
      return `<button class="seat ${busy ? "booked" : ""}" data-id="${s.id}" ${busy ? "disabled" : ""}>${esc(s.seatNumber || s.id)}</button>`;
    })
    .join("");
  box.querySelectorAll(".seat:not(.booked)").forEach((b) =>
    b.addEventListener("click", () => {
      let id = Number(b.dataset.id);
      if (chosen.has(id)) {
        chosen.delete(id);
        b.classList.remove("selected");
      } else if (chosen.size < 4) {
        chosen.add(id);
        b.classList.add("selected");
      } else msg("Maximum 4 seats per reservation.", "error");
      summary();
    }),
  );
  summary();
}
function summary() {
  document.getElementById("selected").textContent =
    seats
      .filter((s) => chosen.has(Number(s.id)))
      .map((s) => s.seatNumber || s.id)
      .join(", ") || "None";
  document.getElementById("amount").textContent = Api.money(
    price * chosen.size,
  );
  document.getElementById("reserve").disabled = !chosen.size;
}
async function reserve() {
  let b = document.getElementById("reserve");
  b.disabled = true;
  try {
    let key = crypto.randomUUID ? crypto.randomUUID() : `web-${Date.now()}`;
    let r = Api.data(
      await Api.request(`/api/shows/${sid}/reservations`, {
        method: "POST",
        headers: { "Idempotency-Key": key },
        body: JSON.stringify({ seatIds: [...chosen].sort((a, b) => a - b) }),
      }),
    );
    msg(
      `Reservation confirmed. Booking #${r.reservationId || r.id || ""}`,
      "success",
    );
    chosen.clear();
    await refresh();
  } catch (e) {
    msg(
      e.status === 409
        ? "Seat unavailable or booking limit reached. Refreshing seats."
        : e.message,
      "error",
    );
    await refresh().catch(() => {});
  } finally {
    summary();
  }
}
