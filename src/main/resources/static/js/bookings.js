document.addEventListener("DOMContentLoaded", async () => {
  if (!auth()) return;
  let box = document.getElementById("bookingList");
  try {
    let x = Api.data(await Api.request("/api/reservations/my"));
    let rows = Array.isArray(x) ? x : x?.content || x?.reservations || [];
    box.innerHTML = rows.length
      ? rows
          .map(
            (r) =>
              `<article class="panel booking"><div><h3>Reservation #${r.reservationId || r.id}</h3><p class="muted">Show #${r.showId} · Seats ${(r.seatIds || []).join(", ")} · ${Api.money(r.amountPaise)}</p><b>${esc(r.status)}</b></div>${String(r.status).toUpperCase() === "CONFIRMED" ? `<button class="btn ghost" data-id="${r.reservationId || r.id}" data-show="${r.showId}">Cancel</button>` : ""}</article>`,
          )
          .join("")
      : "No bookings found.";
    box.querySelectorAll("[data-id]").forEach((b) =>
      b.addEventListener("click", async () => {
        if (!confirm("Cancel reservation?")) return;
        try {
          await Api.request(
            `/api/shows/${b.dataset.show}/reservations/${b.dataset.id}/cancel`,
            { method: "POST", body: "{}" },
          );
          location.reload();
        } catch (e) {
          msg(e.message, "error");
        }
      }),
    );
  } catch (e) {
    msg("Booking history endpoint may differ: " + e.message, "error");
  }
});
