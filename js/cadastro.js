if (CkgdAPI.isAutenticado()) window.location.replace("home.html");
const cadastroForm = document.getElementById("form-cadastro");
const cadastroButton = document.getElementById("btn-finalizar-cadastro");
cadastroForm.addEventListener("submit", async event => {
    event.preventDefault(); if (cadastroButton.disabled) return;
    const error = document.getElementById("form-error"); error.textContent = "";
    const fields = { nomeEmpresa: "nome-empresa", cnpj: "cnpj", email: "email", senha: "senha", pais: "pais", estado: "estado", cidade: "cidade", bairro: "bairro", endereco: "endereco" };
    const payload = Object.fromEntries(Object.entries(fields).map(([key, id]) => [key, key === "senha" ? document.getElementById("input-" + id).value : document.getElementById("input-" + id).value.trim()]));
    payload.cnpj = payload.cnpj.replace(/\D/g, "");
    if (payload.cnpj.length !== 14) { error.textContent = "Informe os 14 dígitos do CNPJ."; return; }
    OasisUI.busy(cadastroButton, true, "Criando conta…");
    try { const auth = await CkgdAPI.cadastrar(payload); CkgdAPI.salvarSessao(auth); sessionStorage.setItem("oasis_welcome", "1"); window.location.href = "home.html"; }
    catch (err) { error.textContent = err.message; }
    finally { OasisUI.busy(cadastroButton, false); }
});
