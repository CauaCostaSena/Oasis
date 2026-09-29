CkgdAPI.exigirAutenticacaoEmpresa();
OasisUI.empresa().catch(() => {});
let modo = 'favoritos';
let requestNumber = 0;
async function carregarFavoritos() {
    const numero = ++requestNumber;
    const lista = document.getElementById('favoritos-lista');
    const loading = document.getElementById('favoritos-carregando');
    const vazio = document.getElementById('favoritos-vazio');
    const erro = document.getElementById('favoritos-erro');
    lista.replaceChildren(); loading.hidden = false; vazio.hidden = true; erro.textContent = '';
    try {
        const items = await (modo === 'favoritos' ? CkgdAPI.listarFavoritos() : CkgdAPI.listarAvaliacoes());
        if (numero !== requestNumber) return;
        vazio.hidden = items.length > 0;
        vazio.querySelector('.status-text').textContent = modo === 'favoritos' ? 'Você ainda não adicionou candidatos aos favoritos.' : 'Sua empresa ainda não avaliou candidatos.';
        items.forEach(f => {
            const id = OasisUI.id(f); const card = OasisUI.el('article','favorito-card');
            card.append(OasisUI.el('h2','',f.nomeCandidato || f.username),OasisUI.el('p','username','@' + f.username));
            if (f.nota != null || f.comentario) card.append(OasisUI.el('span','badge','Avaliação privada'));
            if (f.nota != null) card.append(OasisUI.el('p','rating','Nota: ' + f.nota + ' / 5'));
            if (f.comentario) card.append(OasisUI.el('p','comentario',f.comentario));
            const actions = OasisUI.el('div','saved-card-actions'); const a = OasisUI.el('a','text-link','Ver perfil →'); a.href = 'perfil.html?nodeId=' + encodeURIComponent(id); actions.append(a);
            if (f.favorito) {
                const remove = OasisUI.el('button','btn btn-secondary','Remover favorito'); remove.type = 'button';
                remove.addEventListener('click',async () => { if (remove.disabled) return; remove.disabled=true;
                    try { await CkgdAPI.removerFavorito(id); OasisUI.toast('Favorito removido. Sua avaliação foi preservada.','success'); await carregarFavoritos(); }
                    catch(err) { OasisUI.toast(err.message,'error'); } finally { remove.disabled=false; }
                }); actions.append(remove);
            }
            card.append(actions); lista.append(card);
        });
    } catch(err) { if (numero === requestNumber) erro.textContent = err.message; }
    finally { if (numero === requestNumber) loading.hidden = true; }
}
['favoritos','avaliacoes'].forEach(tab => document.getElementById('tab-' + tab).addEventListener('click',() => {
    modo = tab; ['favoritos','avaliacoes'].forEach(t => document.getElementById('tab-' + t).setAttribute('aria-pressed',String(t === modo))); carregarFavoritos();
}));
carregarFavoritos();
