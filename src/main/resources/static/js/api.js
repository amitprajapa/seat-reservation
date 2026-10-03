const API_BASE =
  location.protocol === "file:" ? "http://localhost:8080" : location.origin;

const Api = {

  token() {
    return localStorage.getItem("seat_token");
  },

  async request(path, options = {}) {

    let headers = {
      "Content-Type": "application/json",
      ...(options.headers || {}),
    };

    if (this.token()) headers.Authorization = "Bearer " + this.token();

    let r = await fetch(API_BASE + path, { ...options, headers });

    let t = await r.text(),
      b = null;

    try {
      b = t ? JSON.parse(t) : null;
    } catch {
      b = t;
    }

    if (!r.ok) {
      let e = new Error(b?.message || b?.error || `HTTP ${r.status}`);
      e.status = r.status;
      throw e;
    }

    return b;
  },

  async logout() {
    try {
      await this.request("/api/auth/logout", {
        method: "POST",
      });
    } catch (error) {
      console.error("Logout API error:", error);
    } finally {
      localStorage.removeItem("seat_token");
      localStorage.removeItem("seat_user");

      window.location.href = "login.html";
    }
  },

  data(x) {
    return x && Object.prototype.hasOwnProperty.call(x, "data") ? x.data : x;
  },

  money(p) {
    return new Intl.NumberFormat("en-IN", {
      style: "currency",
      currency: "INR",
    }).format((Number(p) || 0) / 100);
  },

};