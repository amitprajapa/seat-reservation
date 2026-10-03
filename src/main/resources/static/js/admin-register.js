document.getElementById("adminRegisterForm").addEventListener("submit", async (e) => {

    e.preventDefault();

    const form = new FormData(e.target);

    const request = {
        name: form.get("name"),
        email: form.get("email"),
        password: form.get("password")
    };

    const message = document.getElementById("message");

    try {
        const response = await Api.request("/api/auth/admin/register", {
            method: "POST",
            body: JSON.stringify(request)
        });
	debugger;
        message.textContent = response.message;
        message.style.color = "green";

        e.target.reset();

    } catch (error) {
        message.textContent = error.message;
        message.style.color = "red";
    }
});