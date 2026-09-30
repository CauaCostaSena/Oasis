CkgdAPI.exigirAutenticacaoEmpresa();
const saveCompany = document.getElementById('salvar-empresa');
function aplicarLogos(empresa) {
    CkgdAPI.aplicarLogo(document.getElementById('foto-preview'), empresa);
    document.querySelectorAll('[data-company-logo]').forEach(el => CkgdAPI.aplicarLogo(el, empresa));
}
function aplicarDados(empresa) {
    document.querySelectorAll('[data-company-name]').forEach(el => el.textContent=empresa.nomeEmpresa);
    document.getElementById('nome-empresa').value=empresa.nomeEmpresa;
    document.getElementById('telefone').value=empresa.telefone || '';
    document.getElementById('item-email').textContent=empresa.email;
    document.getElementById('item-plano').textContent=empresa.nomePlano || 'Não informado';
    document.getElementById('item-localidade').textContent=[empresa.cidade,empresa.estado,empresa.pais].filter(Boolean).join(', ');
    aplicarLogos(empresa);
    localStorage.setItem('ckgd_nome',empresa.nomeEmpresa);
}
OasisUI.empresa().then(empresa => { aplicarDados(empresa); saveCompany.disabled=false; }).catch(err => document.getElementById('empresa-erro').textContent=err.message);
document.getElementById('form-empresa').addEventListener('submit',async event => {
    event.preventDefault(); if(saveCompany.disabled) return;
    const error=document.getElementById('empresa-erro'); error.textContent=''; OasisUI.busy(saveCompany,true,'Salvando…');
    try { aplicarDados(await CkgdAPI.atualizarPerfil({nomeEmpresa:document.getElementById('nome-empresa').value.trim(),telefone:document.getElementById('telefone').value.trim()})); OasisUI.toast('Dados atualizados.','success'); }
    catch(err) { error.textContent=err.message; } finally { OasisUI.busy(saveCompany,false); }
});
document.getElementById('input-foto').addEventListener('change',async event => {
    const input=event.target; const file=input.files[0]; if(!file) return;
    const state=document.getElementById('foto-estado');
    if(file.size > 5*1024*1024) { state.textContent='A imagem deve ter no máximo 5 MB.'; input.value=''; return; }
    input.disabled=true; state.textContent='Enviando…';
    try { const empresa=await CkgdAPI.atualizarFoto(file); aplicarLogos(empresa); state.textContent='Foto atualizada.'; }
    catch(err) { state.textContent=err.message; } finally { input.disabled=false; input.value=''; }
});
document.getElementById('form-senha').addEventListener('submit',async event => {
    event.preventDefault(); const button=document.getElementById('salvar-senha'); if(button.disabled) return;
    const error=document.getElementById('senha-erro'); error.textContent='';
    const senhaAtual=document.getElementById('input-senha-atual').value; const novaSenha=document.getElementById('input-nova-senha').value;
    if(novaSenha !== document.getElementById('input-confirmar-senha').value) { error.textContent='A confirmação não confere com a nova senha.'; return; }
    OasisUI.busy(button,true,'Salvando…');
    try { await CkgdAPI.alterarSenha({senhaAtual,novaSenha}); event.target.reset(); OasisUI.toast('Senha alterada.','success'); }
    catch(err) { error.textContent=err.message; } finally { OasisUI.busy(button,false); }
});
