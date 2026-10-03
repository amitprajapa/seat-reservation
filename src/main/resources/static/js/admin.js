document.addEventListener("DOMContentLoaded", () => {
  if (!auth()) return;
  document.getElementById("showForm").addEventListener("submit", async (e) => {
    e.preventDefault();
	const form = e.currentTarget;
    let f = new FormData(form);
    try {
      let x = Api.data(
        await Api.request("/api/admin/shows", {
          method: "POST",
          body: JSON.stringify({
            name: f.get("name"),
            pricePaise: Number(f.get("pricePaise")),
            perUserLimit: Number(f.get("perUserLimit")),
          }),
        }),
      );
	  debugger;
      msg("Show created. ID: " + (x?.id || "see response"), "success");
     form.reset();
    } catch (x) {
      msg(x.message, "error");
    }
  });
  document.getElementById("seatForm").addEventListener("submit", async (e) => {
    e.preventDefault();
    let f = new FormData(e.currentTarget);
    let seatNumbers = f
      .get("seatNumbers")
      .split(",")
      .map((x) => x.trim())
      .filter(Boolean);
    try {
      await Api.request(`/api/admin/shows/${f.get("showId")}/seats`, {
        method: "POST",
        body: JSON.stringify({ seatNumbers }),
      });
      msg("Seat creation request completed.", "success");
    } catch (x) {
      msg(x.message, "error");
    }
  });
});
