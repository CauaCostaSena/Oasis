const CkgdSuporte = {
    abrirModal() {
        const modal = OasisUI.modal("Falar com o suporte", "Registre uma dúvida ou solicitação vinculada à sua empresa.");
        const form = OasisUI.el("form");
        const assunto = OasisUI.field("Assunto", "suporte-assunto"); assunto.input.required = true; assunto.input.maxLength = 150;
        const mensagem = OasisUI.field("Mensagem", "suporte-mensagem", "textarea"); mensagem.input.required = true; mensagem.input.maxLength = 5000;
        const error = OasisUI.el("p", "form-error"); error.setAttribute("role", "alert");
        const actions = OasisUI.el("div", "modal-actions");
        const cancel = OasisUI.el("button", "btn btn-secondary", "Cancelar"); cancel.type = "button"; cancel.addEventListener("click", modal.close);
        const send = OasisUI.el("button", "btn btn-primary", "Enviar solicitação"); send.type = "submit";
        actions.append(cancel, send); form.append(assunto.wrap, mensagem.wrap, error, actions); modal.content.append(form);
        form.addEventListener("submit", async event => {
            event.preventDefault(); if (send.disabled) return;
            error.textContent = ""; OasisUI.busy(send, true, "Enviando…");
            try { await CkgdAPI.enviarSuporte({ assunto: assunto.input.value.trim(), mensagem: mensagem.input.value.trim() }); modal.close(); OasisUI.toast("Solicitação registrada com sucesso.", "success"); }
            catch (err) { error.textContent = err.message; }
            finally { OasisUI.busy(send, false); }
        }); modal.open();
    }
};
