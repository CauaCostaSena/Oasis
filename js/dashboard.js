CkgdAPI.exigirAutenticacaoEmpresa();

if (sessionStorage.getItem("oasis_welcome")) {
    OasisUI.toast("Conta criada. Seu plano gratuito já está ativo.", "success");
    sessionStorage.removeItem("oasis_welcome");
}

function textOrDash(value) { return value === undefined || value === null ? "—" : String(value); }
function formatFilters(item) {
    const values = [];
    if (item.linguagem) values.push(item.linguagem);
    if (item.localizacao) values.push(item.localizacao);
    return values.length ? values.join(" · ") : "Sem filtros adicionais";
}
function formatDate(item) {
    if (!item.data) return "—";
    const [y,m,d] = item.data.split("-");
    const hora = item.hora ? item.hora.slice(0,5) : "";
    return [d,m,y].join("/") + (hora ? " · " + hora : "");
}

async function carregarDashboard() {
    const [empresaResult, favoritosResult, avaliacoesResult, historicoResult] = await Promise.allSettled([
        OasisUI.empresa(),
        CkgdAPI.listarFavoritos(),
        CkgdAPI.listarAvaliacoes(),
        CkgdAPI.historicoBuscas()
    ]);

    if (empresaResult.status === "fulfilled") {
        document.getElementById("metric-plano").textContent = empresaResult.value.nomePlano || "Não informado";
    }
    document.getElementById("metric-favoritos").textContent = favoritosResult.status === "fulfilled" ? favoritosResult.value.length : "—";
    document.getElementById("metric-avaliacoes").textContent = avaliacoesResult.status === "fulfilled" ? avaliacoesResult.value.length : "—";

    const loading = document.getElementById("historico-loading");
    const empty = document.getElementById("historico-empty");
    const wrap = document.getElementById("historico-wrap");
    const error = document.getElementById("historico-error");
    loading.hidden = true;

    if (historicoResult.status === "rejected") {
        document.getElementById("metric-buscas").textContent = "—";
        error.textContent = historicoResult.reason.message;
        return;
    }

    const historico = historicoResult.value;
    document.getElementById("metric-buscas").textContent = textOrDash(historico.length);
    if (!historico.length) { empty.hidden = false; return; }

    const tbody = document.getElementById("historico-body");
    historico.slice(0, 8).forEach(item => {
        const tr = document.createElement("tr");
        const term = document.createElement("td");
        const strong = document.createElement("strong");
        strong.textContent = item.termo || "Busca geral";
        term.append(strong);
        const filters = document.createElement("td");
        filters.textContent = formatFilters(item);
        const date = document.createElement("td");
        date.textContent = formatDate(item);
        tr.append(term, filters, date);
        tbody.append(tr);
    });
    wrap.hidden = false;
}

carregarDashboard();
