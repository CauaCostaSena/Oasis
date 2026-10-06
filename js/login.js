if (CkgdAPI.isAutenticado()) window.location.replace("dashboard.html");
const loginForm = document.getElementById("form-login");
const loginButton = document.getElementById("btn-login");
const loginError = document.getElementById("form-error");
if (new URLSearchParams(location.search).get("motivo") === "sessao") loginError.textContent = "Sua sessão expirou. Entre novamente para continuar.";
loginForm.addEventListener("submit", async event => {
    event.preventDefault(); if (loginButton.disabled) return;
    loginError.textContent = ""; OasisUI.busy(loginButton, true, "Entrando…");
    try {
        const auth = await CkgdAPI.login(document.getElementById("input-usuario").value.trim(), document.getElementById("input-senha").value);
        CkgdAPI.salvarSessao(auth); window.location.href = "dashboard.html";
    } catch (err) { loginError.textContent = err.message; }
    finally { OasisUI.busy(loginButton, false); }
});
