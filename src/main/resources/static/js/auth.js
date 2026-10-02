document.addEventListener("DOMContentLoaded", () => {
  let l = document.getElementById("loginForm");
  if (l)
    l.addEventListener("submit", async (e) => {
      e.preventDefault();
      let f = new FormData(l);
      try {
        let b = await Api.request("/api/auth/login", {
          method: "POST",
          body: JSON.stringify({
            email: f.get("email"),
            password: f.get("password"),
          }),
        });
        let d = Api.data(b),
          token = d?.token || d?.accessToken || d?.jwt || b?.token;
        if (!token)
          throw Error(
            "No JWT token found in login response; adjust auth.js to match backend DTO.",
          );
        localStorage.setItem("seat_token", token);
        location.href = "index.html";
      } catch (x) {
        msg(x.message, "error");
      }
    });
  let r = document.getElementById("registerForm");
  if (r)
    r.addEventListener("submit", async (e) => {
      e.preventDefault();
      let f = new FormData(r);
      try {
        await Api.request("/api/auth/register", {
          method: "POST",
          body: JSON.stringify({
            name: f.get("name"),
            email: f.get("email"),
            password: f.get("password"),
          }),
        });
        msg("Account created. Please sign in.", "success");
        setTimeout(() => (location.href = "login.html"), 800);
      } catch (x) {
        msg(x.message, "error");
      }
    });
});
