// Oásis — cliente HTTP. Chaves ckgd_* mantidas para compatibilidade das sessões.
const API_BASE_URL = (window.OASIS_API_BASE_URL || "http://localhost:8080/api").replace(/\/$/, "");
class OasisApiError extends Error {
    constructor(message, status = 0) { super(message); this.name = "OasisApiError"; this.status = status; }
}
const CkgdAPI = {
    _token() { return localStorage.getItem("ckgd_token"); },
    isAutenticado() {
        const token = this._token();
        if (!token) return false;
        if (localStorage.getItem("ckgd_tipo") === "CANDIDATO") { this.encerrarSessao(); return false; }
        try {
            const part = token.split(".")[1].replace(/-/g, "+").replace(/_/g, "/");
            const claims = JSON.parse(atob(part));
            if (claims.tipo !== "EMPRESA" || !/^[0-9]{14}$/.test(claims.sub || "") || !claims.exp || claims.exp * 1000 <= Date.now()) { this.encerrarSessao(); return false; }
            return true;
        } catch (_) { this.encerrarSessao(); return false; }
    },
    salvarSessao(auth) {
        if (!auth.token) throw new Error("O servidor não retornou uma sessão válida.");
        localStorage.setItem("ckgd_token", auth.token);
        localStorage.setItem("ckgd_tipo", "EMPRESA");
        localStorage.setItem("ckgd_cnpj", auth.cnpj || "");
        localStorage.setItem("ckgd_nome", auth.nome || "");
        localStorage.setItem("ckgd_email", auth.email || "");
        localStorage.removeItem("ckgd_nodeId");
    },
    encerrarSessao() { ["token", "tipo", "cnpj", "nodeId", "nome", "email"].forEach(k => localStorage.removeItem("ckgd_" + k)); },
    nomeEmpresaLogada() { return localStorage.getItem("ckgd_nome") || ""; },
    exigirAutenticacaoEmpresa() {
        if (this.isAutenticado()) return true;
        window.location.replace("login.html");
        return false;
    },
    urlArquivo(path) {
        if (!path || !String(path).startsWith("/uploads/")) return null;
        return new URL(path, API_BASE_URL).href;
    },
    aplicarLogo(el, empresa) {
        if (!el) return;
        el.replaceChildren();
        const url = this.urlArquivo(empresa.fotoUrl);
        if (url) {
            const img = document.createElement("img"); img.src = url; img.alt = "";
            img.addEventListener("error", () => { el.textContent = String(empresa.nomeEmpresa || "E").slice(0, 2).toUpperCase(); });
            el.append(img);
        } else el.textContent = String(empresa.nomeEmpresa || "E").slice(0, 2).toUpperCase();
    },
    async _request(method, path, body, options = {}) {
        const controller = new AbortController();
        const timeout = setTimeout(() => controller.abort(), options.timeout || 60000);
        const token = this._token();
        const headers = { Accept: "application/json" };
        if (token) headers.Authorization = "Bearer " + token;
        if (body !== undefined && !(body instanceof FormData)) headers["Content-Type"] = "application/json";
        try {
            const response = await fetch(API_BASE_URL + path, {
                method, headers, signal: controller.signal,
                body: body === undefined ? undefined : body instanceof FormData ? body : JSON.stringify(body)
            });
            if (response.status === 204) return null;
            const data = await response.json().catch(() => null);
            if (!response.ok) {
                if (response.status === 401 && token && !path.startsWith("/auth/")) {
                    this.encerrarSessao(); window.location.replace("login.html?motivo=sessao");
                }
                const fallback = response.status === 429 ? "Muitas solicitações. Aguarde um momento e tente novamente." : "Não foi possível concluir a solicitação.";
                throw new OasisApiError(data?.message || data?.mensagem || fallback, response.status);
            }
            return data;
        } catch (err) {
            if (err instanceof OasisApiError) throw err;
            if (err.name === "AbortError") throw new OasisApiError("A consulta demorou mais que o esperado. Tente novamente em instantes.");
            throw new OasisApiError("Não foi possível conectar ao servidor. Verifique sua conexão e tente novamente.");
        } finally { clearTimeout(timeout); }
    },
    login(email, senha) { return this._request("POST", "/auth/login", { email, senha }); },
    cadastrar(payload) { return this._request("POST", "/auth/cadastro", payload); },
    meusDados() { return this._request("GET", "/empresas/me"); },
    atualizarPerfil(payload) { return this._request("PUT", "/empresas/me", payload); },
    alterarSenha(payload) { return this._request("PUT", "/empresas/me/senha", payload); },
    atualizarFoto(arquivo) { const data = new FormData(); data.append("arquivo", arquivo); return this._request("POST", "/empresas/me/foto", data); },
    listarPlanos() { return this._request("GET", "/planos"); },
    buscarCandidatos({ termo, linguagem, localizacao }) {
        const params = new URLSearchParams();
        if (termo) params.set("termo", termo);
        if (linguagem) params.set("linguagem", linguagem);
        if (localizacao) params.set("localizacao", localizacao);
        return this._request("GET", "/busca?" + params);
    },
    perfilCandidato(id) { return this._request("GET", "/candidatos/" + encodeURIComponent(id)); },
    listarFavoritos() { return this._request("GET", "/favoritos"); },
    definirFavorito(id, favorito) { return this._request("PUT", "/favoritos/" + encodeURIComponent(id), { favorito }); },
    removerFavorito(id) { return this._request("DELETE", "/favoritos/" + encodeURIComponent(id)); },
    listarAvaliacoes() { return this._request("GET", "/avaliacoes"); },
    avaliacaoCandidato(id) { return this._request("GET", "/avaliacoes/" + encodeURIComponent(id)); },
    salvarAvaliacao(id, payload) { return this._request("PUT", "/avaliacoes/" + encodeURIComponent(id), payload); },
    enviarSuporte(payload) { return this._request("POST", "/suporte", payload); }
};
