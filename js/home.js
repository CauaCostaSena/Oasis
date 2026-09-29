CkgdAPI.exigirAutenticacaoEmpresa();
OasisUI.empresa().catch(() => {});
if (sessionStorage.getItem('oasis_welcome')) { OasisUI.toast('Conta criada. Seu plano gratuito já está ativo.', 'success'); sessionStorage.removeItem('oasis_welcome'); }
let linguagemSelecionada = null;
let buscando = false;
const buttons = ['btn-buscar','btn-filtrar','btn-limpar-filtros'].map(id => document.getElementById(id));
function mostrarEstado(estado) {
    ['aguardando','carregando','erro','resultados'].forEach(nome => document.getElementById('estado-' + nome).hidden = nome !== estado);
}
function renderizarResultados(candidatos) {
    const lista = document.getElementById('results-list'); lista.replaceChildren();
    document.getElementById('results-summary').textContent = candidatos.length ? candidatos.length + ' perfis encontrados nesta consulta' : 'Nenhum candidato encontrado. Ajuste os filtros e tente novamente.';
    candidatos.forEach(c => {
        const id = OasisUI.id(c);
        const card = OasisUI.el('article','candidato-card');
        const header = OasisUI.el('div','candidate-header');
        const title = OasisUI.el('div','candidate-title');
        title.append(OasisUI.el('h3','',c.nomeCandidato || c.username),OasisUI.el('p','','@' + c.username));
        header.append(OasisUI.avatar(c.avatarUrl,c.nomeCandidato || c.username),title);
        const tags = OasisUI.el('div','candidate-tags');
        (c.linguagensIdentificadas || []).slice(0,3).forEach(l => tags.append(OasisUI.el('span','badge',l)));
        const actions = OasisUI.el('div','candidate-actions');
        const link = OasisUI.el('a','text-link','Ver perfil →'); link.href = 'perfil.html?nodeId=' + encodeURIComponent(id);
        const favorite = OasisUI.el('button','btn-favorite'); favorite.type = 'button';
        let selected = Boolean(c.favorito);
        const update = () => { favorite.textContent = selected ? 'Remover favorito' : 'Favoritar'; favorite.setAttribute('aria-pressed',String(selected)); }; update();
        favorite.addEventListener('click',async () => {
            if (favorite.disabled) return; favorite.disabled = true;
            try { await CkgdAPI.definirFavorito(id,!selected); selected = !selected; update(); OasisUI.toast(selected ? 'Candidato adicionado aos favoritos.' : 'Favorito removido.','success'); }
            catch(err) { OasisUI.toast(err.message,'error'); }
            finally { favorite.disabled = false; }
        });
        actions.append(link,favorite);
        card.append(header,OasisUI.el('p','candidate-location',c.localizacao || 'Localização não informada'),tags,OasisUI.el('p','candidate-meta',(c.numRepositorios ?? 0) + ' repositórios públicos · linguagens da amostra analisada'),actions);
        lista.append(card);
    });
}
async function executarBusca(event) {
    event?.preventDefault(); if (buscando) return; buscando = true;
    buttons.forEach(b => b.disabled = true); mostrarEstado('carregando');
    try {
        const candidatos = await CkgdAPI.buscarCandidatos({ termo: document.getElementById('input-termo').value.trim(), linguagem: linguagemSelecionada, localizacao: document.getElementById('input-localizacao').value.trim() });
        renderizarResultados(candidatos); mostrarEstado('resultados');
    } catch(err) { document.getElementById('erro-mensagem').textContent = err.message; mostrarEstado('erro'); }
    finally { buscando = false; buttons.forEach(b => b.disabled = false); }
}
document.getElementById('form-busca').addEventListener('submit',executarBusca);
document.getElementById('btn-filtrar').addEventListener('click',executarBusca);
document.getElementById('lang-toggles').addEventListener('click',event => {
    const button = event.target.closest('button[data-lang]'); if (!button || buscando) return;
    linguagemSelecionada = linguagemSelecionada === button.dataset.lang ? null : button.dataset.lang;
    document.querySelectorAll('[data-lang]').forEach(b => b.setAttribute('aria-pressed',String(b.dataset.lang === linguagemSelecionada)));
});
document.getElementById('btn-limpar-filtros').addEventListener('click',() => {
    linguagemSelecionada = null; document.getElementById('input-localizacao').value = ''; document.getElementById('input-termo').value = '';
    document.querySelectorAll('[data-lang]').forEach(b => b.setAttribute('aria-pressed','false')); mostrarEstado('aguardando');
});
