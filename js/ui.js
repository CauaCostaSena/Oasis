// Componentes pequenos compartilhados; dados externos sempre entram como texto.
const OasisUI = {
    el(tag, className, text) {
        const element = document.createElement(tag);
        if (className) element.className = className;
        if (text !== undefined && text !== null) element.textContent = String(text);
        return element;
    },
    id(candidate) { return candidate.githubUserId ?? candidate.nodeId ?? candidate.nodeIdCandidato; },
    githubUrl(username) { return "https://github.com/" + encodeURIComponent(username || ""); },
    avatar(url, name) {
        const wrap = this.el("div", "avatar");
        wrap.setAttribute("aria-hidden", "true");
        wrap.textContent = String(name || "GH").slice(0, 2).toUpperCase();
        try {
            const safe = new URL(url);
            if (safe.protocol !== "https:" || safe.hostname !== "avatars.githubusercontent.com") return wrap;
            const img = this.el("img"); img.src = safe.href; img.alt = ""; img.loading = "lazy";
            img.addEventListener("error", () => { img.remove(); });
            wrap.append(img);
        } catch (_) { /* Iniciais já disponíveis. */ }
        return wrap;
    },
    toast(message, type = "info") {
        let region = document.getElementById("toast-region");
        if (!region) { region = this.el("div", "toast-region"); region.id = "toast-region"; region.setAttribute("aria-live", "polite"); region.setAttribute("aria-atomic", "false"); document.body.append(region); }
        const toast = this.el("div", "toast toast-" + type);
        toast.append(this.el("span", "", message));
        const close = this.el("button", "toast-close", "×"); close.type = "button"; close.setAttribute("aria-label", "Fechar mensagem");
        close.addEventListener("click", () => toast.remove()); toast.append(close); region.append(toast);
        if (type !== "error") setTimeout(() => toast.remove(), 7000);
        while (region.childElementCount > 4) region.firstElementChild.remove();
    },
    busy(button, active, text = "Aguarde…") {
        if (active) { button.dataset.originalText = button.textContent; button.textContent = text; }
        else if (button.dataset.originalText) button.textContent = button.dataset.originalText;
        button.disabled = active; button.setAttribute("aria-busy", String(active));
    },
    modal(title, description) {
        const previous = document.activeElement;
        const dialog = this.el("dialog", "oasis-modal");
        const heading = this.el("h2", "", title); heading.id = "modal-title";
        dialog.setAttribute("aria-labelledby", heading.id); dialog.setAttribute("aria-modal", "true");
        dialog.append(heading);
        if (description) dialog.append(this.el("p", "muted", description));
        const content = this.el("div", "modal-content"); dialog.append(content);
        const close = () => dialog.close();
        dialog.addEventListener("click", e => { if (e.target === dialog) { const box = dialog.getBoundingClientRect(); if (e.clientX < box.left || e.clientX > box.right || e.clientY < box.top || e.clientY > box.bottom) close(); } });
        dialog.addEventListener("close", () => { dialog.remove(); document.body.classList.remove("modal-open"); previous?.focus(); });
        return { dialog, content, close, open: () => { document.body.append(dialog); document.body.classList.add("modal-open"); dialog.showModal(); (dialog.querySelector("input, textarea, button") || heading).focus(); } };
    },
    field(labelText, name, type = "text", value = "") {
        const wrap = this.el("div", "field");
        const label = this.el("label", "", labelText); label.htmlFor = name;
        const input = this.el(type === "textarea" ? "textarea" : "input"); input.id = name; input.name = name;
        if (type !== "textarea") input.type = type;
        input.value = value; wrap.append(label, input); return { wrap, input };
    },
    async empresa() {
        try {
            const empresa = await CkgdAPI.meusDados();
            document.querySelectorAll("[data-company-name]").forEach(e => e.textContent = empresa.nomeEmpresa);
            document.querySelectorAll("[data-company-location]").forEach(e => e.textContent = [empresa.cidade, empresa.estado].filter(Boolean).join(", ") || "Conta da empresa");
            document.querySelectorAll("[data-company-logo]").forEach(e => CkgdAPI.aplicarLogo(e, empresa));
            return empresa;
        } catch (err) {
            document.querySelectorAll("[data-company-name]").forEach(e => e.textContent = CkgdAPI.nomeEmpresaLogada() || "Minha empresa");
            this.toast(err.message, "error"); throw err;
        }
    },
    initShell() {
        const toggle = document.getElementById("menu-toggle");
        const sidebar = document.getElementById("sidebar");
        const shade = document.getElementById("sidebar-shade");
        const close = () => { document.body.classList.remove("nav-open"); toggle?.setAttribute("aria-expanded", "false"); if (shade) shade.hidden = true; if (window.matchMedia("(max-width: 900px)").matches && sidebar) sidebar.inert = true; };
        toggle?.addEventListener("click", () => {
            if (document.body.classList.contains("nav-open")) { close(); return; }
            document.body.classList.add("nav-open"); toggle.setAttribute("aria-expanded", "true"); shade.hidden = false; sidebar.inert = false; sidebar.querySelector("a,button")?.focus();
        });
        shade?.addEventListener("click", () => { close(); toggle?.focus(); });
        document.addEventListener("keydown", e => {
            if (e.key === "Escape" && document.body.classList.contains("nav-open")) { close(); toggle.focus(); }
            if (e.key === "Tab" && document.body.classList.contains("nav-open")) {
                const focusable = [toggle, ...sidebar.querySelectorAll("a,button")];
                const index = focusable.indexOf(document.activeElement);
                e.preventDefault();
                focusable[(index + (e.shiftKey ? -1 : 1) + focusable.length) % focusable.length].focus();
            }
        });
        const breakpoint = window.matchMedia("(max-width: 900px)");
        const update = () => { close(); if (sidebar) sidebar.inert = breakpoint.matches; };
        breakpoint.addEventListener("change", update); update();
        document.getElementById("menu-sair")?.addEventListener("click", () => { CkgdAPI.encerrarSessao(); window.location.href = "login.html"; });
        document.getElementById("menu-suporte")?.addEventListener("click", () => {
            if (document.body.classList.contains("nav-open")) { close(); toggle?.focus(); }
            CkgdSuporte.abrirModal();
        });
    },
    initPasswords() {
        document.querySelectorAll("[data-password-toggle]").forEach(button => button.addEventListener("click", () => {
            const input = document.getElementById(button.dataset.passwordToggle);
            const visible = input.type === "password"; input.type = visible ? "text" : "password";
            button.textContent = visible ? "Ocultar" : "Mostrar"; button.setAttribute("aria-pressed", String(visible));
        }));
    }
};
OasisUI.initPasswords();
if (document.body.classList.contains("app-shell")) OasisUI.initShell();
