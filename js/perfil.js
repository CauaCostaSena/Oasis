CkgdAPI.exigirAutenticacaoEmpresa();
OasisUI.empresa().catch(() => {});
const nodeId = new URLSearchParams(location.search).get('nodeId');
const nota = document.getElementById('input-nota');
const comentario = document.getElementById('input-comentario');
const save = document.getElementById('btn-salvar-perfil');
const state = document.getElementById('avaliacao-estado');
let favorito = false;
const favButton = document.getElementById('btn-favoritar');
function updateFavorite() { favButton.textContent = favorito ? 'Remover favorito' : 'Favoritar'; favButton.setAttribute('aria-pressed',String(favorito)); }
function renderRepos(repos) {
    const lista = document.getElementById('perfil-repos-lista'); lista.replaceChildren();
    if (!repos.length) lista.append(OasisUI.el('p','muted','Nenhum repositório público na amostra sincronizada.'));
    repos.forEach(r => {
        const item = OasisUI.el('article','repo-item');
        const heading = OasisUI.el('h3','',r.nomeRepositorio);
        item.append(heading,OasisUI.el('p','',r.descricao || 'Sem descrição'),OasisUI.el('div','repo-stats',(r.linguagemPrincipal || 'Linguagem não informada') + ' · Estrelas: ' + (r.numeroEstrela ?? 0) + ' · Forks: ' + (r.numeroFork ?? 0) + ' · Issues: ' + (r.numeroIssue ?? 0)));
        if (r.ultimoCommit) item.append(OasisUI.el('p','repo-updated','Último push informado (UTC): ' + r.ultimoCommit.replace('T',' ')));
        try { const url = new URL(r.urlRepositorio); if (url.protocol === 'https:' && url.hostname === 'github.com') { const a = OasisUI.el('a','text-link','Abrir repositório →'); a.href=url.href; a.target='_blank'; a.rel='noopener noreferrer'; item.append(a); } } catch (_) {}
        lista.append(item);
    });
}
async function carregarPerfil() {
    try {
        if (!nodeId || !/^[0-9]+$/.test(nodeId)) throw new Error('Candidato não especificado ou inválido.');
        const c = await CkgdAPI.perfilCandidato(nodeId);
        document.getElementById('perfil-avatar').replaceChildren(OasisUI.avatar(c.avatarUrl,c.nomeCandidato || c.username));
        document.getElementById('perfil-nome').textContent = c.nomeCandidato || c.username;
        document.getElementById('perfil-username').textContent = '@' + c.username;
        document.getElementById('perfil-localizacao').textContent = c.localizacao || 'Localização não informada';
        document.getElementById('perfil-bio').textContent = c.bio || 'Sem biografia informada.';
        document.getElementById('perfil-repos').textContent = c.numRepositorios ?? 0;
        document.getElementById('perfil-estrelas').textContent = c.totalEstrelas ?? 0;
        document.getElementById('perfil-amostra').textContent = c.repositoriosAnalisados ?? 0;
        document.getElementById('perfil-linguagem').textContent = 'Linguagem mais frequente na amostra: ' + (c.linguagemPrincipal || 'não identificada');
        document.getElementById('perfil-github-link').href = OasisUI.githubUrl(c.username);
        const badges = document.getElementById('perfil-badges'); badges.replaceChildren();
        (c.linguagensIdentificadas || []).forEach(l => badges.append(OasisUI.el('span','badge',l)));
        renderRepos(c.repositorios || []); favorito = Boolean(c.favorito); updateFavorite();
        document.getElementById('perfil-container').hidden = false;
        // Nunca liberar edição após falha de leitura: evita sobrescrever avaliação desconhecida.
        try { const a = await CkgdAPI.avaliacaoCandidato(nodeId); nota.value = a.nota ?? ''; comentario.value = a.comentario || ''; save.disabled = false; }
        catch(err) { if (err.status === 404) save.disabled = false; else state.textContent = 'Não foi possível carregar sua avaliação. Recarregue a página antes de editar. ' + err.message; }
    } catch(err) { document.getElementById('perfil-erro-msg').textContent = err.message; document.getElementById('perfil-erro').hidden = false; }
    finally { document.getElementById('perfil-carregando').hidden = true; }
}
favButton.addEventListener('click',async () => {
    if (favButton.disabled) return; favButton.disabled = true;
    try { await CkgdAPI.definirFavorito(nodeId,!favorito); favorito = !favorito; updateFavorite(); OasisUI.toast(favorito ? 'Candidato adicionado aos favoritos.' : 'Favorito removido.','success'); }
    catch(err) { OasisUI.toast(err.message,'error'); } finally { favButton.disabled = false; }
});
document.getElementById('form-avaliacao').addEventListener('submit',async event => {
    event.preventDefault(); if (save.disabled) return;
    if (!nota.value && !comentario.value.trim()) { state.textContent = 'Informe uma nota ou um comentário.'; return; }
    OasisUI.busy(save,true,'Salvando…'); state.textContent = '';
    try { await CkgdAPI.salvarAvaliacao(nodeId,{ nota: nota.value ? Number(nota.value) : null, comentario: comentario.value.trim() }); state.textContent = 'Avaliação privada salva.'; OasisUI.toast('Avaliação salva.','success'); }
    catch(err) { state.textContent = err.message; }
    finally { OasisUI.busy(save,false); }
});
carregarPerfil();
