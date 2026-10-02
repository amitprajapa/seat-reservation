document.addEventListener("DOMContentLoaded", () => {
  let n = document.getElementById("nav");
  if (!n) return;
  let signed = !!Api.token();
n.innerHTML = `<header class="top"><a class="brand" href="index.html">seat<span>ly.</span></a><nav><a href="index.html">Shows</a>${signed ? '<a href="bookings.html">Bookings</a><a href="#" id="logout">Sign out</a>' : '<a href="login.html">Sign in</a><a href="register.html">Register</a>'}</nav></header>`;  document.getElementById("logout")?.addEventListener("click", (e) => {
    e.preventDefault();
    localStorage.removeItem("seat_token");
    location.href = "login.html";
  });
});
function msg(t, type = "") {
  let e = document.getElementById("message");
  if (e) e.innerHTML = `<div class="notice ${type}">${esc(t)}</div>`;
}
function esc(s) {
  return String(s ?? "").replace(
    /[&<>"']/g,
    (c) =>
      ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" })[
        c
      ],
  );
}
function auth() {
  if (!Api.token()) {
    location.href = "login.html";
    return false;
  }
  return true;
}
