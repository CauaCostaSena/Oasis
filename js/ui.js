// Oásis — componentes e utilidades de interface. Dados externos entram como texto.
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
            img.addEventListener("error", () => img.remove());
            wrap.append(img);
        } catch (_) {}
        return wrap;
    },
    toast(message, type = "info") {
        let region = document.getElementById("toast-region");
        if (!region) {
            region = this.el("div", "toast-region");
            region.id = "toast-region";
            region.setAttribute("aria-live", "polite");
            region.setAttribute("aria-atomic", "false");
            document.body.append(region);
        }
        const toast = this.el("div", "toast toast-" + type);
        toast.append(this.el("span", "", message));
        const close = this.el("button", "toast-close", "×");
        close.type = "button"; close.setAttribute("aria-label", "Fechar mensagem");
        close.addEventListener("click", () => toast.remove());
        toast.append(close); region.append(toast);
        if (type !== "error") setTimeout(() => toast.remove(), 7000);
        while (region.childElementCount > 4) region.firstElementChild.remove();
    },
    busy(button, active, text = "Aguarde…") {
        if (!button) return;
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
        dialog.addEventListener("click", e => {
            if (e.target !== dialog) return;
            const box = dialog.getBoundingClientRect();
            if (e.clientX < box.left || e.clientX > box.right || e.clientY < box.top || e.clientY > box.bottom) close();
        });
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
            document.querySelectorAll("[data-company-plan]").forEach(e => e.textContent = empresa.nomePlano || "Plano");
            document.querySelectorAll("[data-company-logo]").forEach(e => CkgdAPI.aplicarLogo(e, empresa));
            return empresa;
        } catch (err) {
            document.querySelectorAll("[data-company-name]").forEach(e => e.textContent = CkgdAPI.nomeEmpresaLogada() || "Minha empresa");
            this.toast(err.message, "error"); throw err;
        }
    },
    globalLoader: {
        show(text = "Carregando dados do Oásis…") {
            let overlay = document.getElementById("global-loader");
            if (!overlay) {
                overlay = document.createElement("div");
                overlay.id = "global-loader"; overlay.className = "global-loader";
                overlay.setAttribute("role", "status"); overlay.setAttribute("aria-live", "polite");
                const box = document.createElement("div"); box.className = "global-loader-box";
                const orbit = document.createElement("div"); orbit.className = "oasis-orbit"; orbit.setAttribute("aria-hidden", "true");
                for (let i = 0; i < 4; i++) orbit.append(document.createElement("span"));
                const p = document.createElement("p"); p.id = "global-loader-text";
                box.append(orbit, p); overlay.append(box); document.body.append(overlay);
            }
            document.getElementById("global-loader-text").textContent = text;
            overlay.hidden = false;
        },
        hide() { const overlay = document.getElementById("global-loader"); if (overlay) overlay.hidden = true; }
    },
    comparison: {
        key: "oasis_compare_ids",
        ids() {
            try { return JSON.parse(sessionStorage.getItem(this.key) || "[]").map(String).filter(id => /^\d+$/.test(id)).slice(0, 3); }
            catch (_) { return []; }
        },
        save(ids) {
            const clean = [...new Set(ids.map(String).filter(id => /^\d+$/.test(id)))].slice(0, 3);
            sessionStorage.setItem(this.key, JSON.stringify(clean));
            OasisUI.updateCompareIndicator();
            return clean;
        },
        has(id) { return this.ids().includes(String(id)); },
        toggle(id) {
            const value = String(id); const ids = this.ids();
            if (ids.includes(value)) return this.save(ids.filter(item => item !== value));
            if (ids.length >= 3) throw new Error("Selecione no máximo 3 candidatos para comparar.");
            return this.save([...ids, value]);
        },
        clear() { this.save([]); }
    },
    updateCompareIndicator() {
        const ids = this.comparison.ids();
        let box = document.getElementById("compare-indicator");
        if (!ids.length || document.body.dataset.page === "comparacao") { if (box) box.hidden = true; return; }
        if (!box) {
            box = this.el("div", "compare-indicator"); box.id = "compare-indicator";
            const text = this.el("span"); text.id = "compare-indicator-text";
            const link = this.el("a", "", "Comparar agora →"); link.href = "comparacao.html";
            box.append(text, link); document.body.append(box);
        }
        document.getElementById("compare-indicator-text").textContent = ids.length + (ids.length === 1 ? " candidato selecionado" : " candidatos selecionados");
        box.hidden = false;
    },
    buildSidebar() {
        const sidebar = document.getElementById("sidebar");
        if (!sidebar) return;
        const current = document.body.dataset.page || "";
        sidebar.replaceChildren();
        const brand = this.el("a", "brand", "Oásis"); brand.href = "dashboard.html"; brand.setAttribute("aria-label", "Oásis, início");
        const caption = this.el("p", "nav-caption", "NAVEGAÇÃO");
        const nav = this.el("nav", "menu-card"); nav.setAttribute("aria-label", "Menu principal");
        const items = [["dashboard", "⌂", "Início", "dashboard.html"],["candidatos", "⌕", "Buscar candidatos", "home.html"],["favoritos", "♡", "Favoritos", "favoritos.html"],["comparacao", "⇄", "Comparar", "comparacao.html"],["insights", "↗", "Insights", "insights.html"],["assinatura", "◈", "Assinatura", "assinatura.html"],["empresa", "▣", "Perfil da empresa", "empresa.html"],["config", "⚙", "Configurações", "config.html"]];
        items.forEach(([key, icon, label, href]) => {
            const a = this.el("a", "menu-item" + (current === key ? " active" : "")); a.href = href;
            if (current === key) a.setAttribute("aria-current", "page");
            a.append(this.el("span", "menu-icon", icon), this.el("span", "", label)); nav.append(a);
        });
        const support = this.el("button", "menu-item"); support.id = "menu-suporte"; support.type = "button";
        support.append(this.el("span", "menu-icon", "?"), this.el("span", "", "Suporte")); nav.append(support);
        const bottom = this.el("div", "sidebar-bottom");
        const company = this.el("a", "company-card"); company.href = "empresa.html";
        const logo = this.el("div", "logo"); logo.dataset.companyLogo = "";
        const info = this.el("div", "company-info");
        const name = this.el("p", "company-name", "Minha empresa"); name.dataset.companyName = "";
        const plan = this.el("p", "company-plan", "Plano"); plan.dataset.companyPlan = "";
        info.append(name, plan); company.append(logo, info);
        const logout = this.el("button", "menu-item"); logout.id = "menu-sair"; logout.type = "button";
        logout.append(this.el("span", "menu-icon", "↪"), this.el("span", "", "Sair")); bottom.append(company, logout);
        sidebar.append(brand, caption, nav, bottom);
    },
    initShell() {
        this.buildSidebar();
        const toggle = document.getElementById("menu-toggle"); const sidebar = document.getElementById("sidebar"); const shade = document.getElementById("sidebar-shade");
        const close = () => { document.body.classList.remove("nav-open"); toggle?.setAttribute("aria-expanded", "false"); if (shade) shade.hidden = true; if (window.matchMedia("(max-width: 900px)").matches && sidebar) sidebar.inert = true; };
        toggle?.addEventListener("click", () => { if (document.body.classList.contains("nav-open")) { close(); return; } document.body.classList.add("nav-open"); toggle.setAttribute("aria-expanded", "true"); shade.hidden = false; sidebar.inert = false; sidebar.querySelector("a,button")?.focus(); });
        shade?.addEventListener("click", () => { close(); toggle?.focus(); });
        document.addEventListener("keydown", e => {
            if (e.key === "Escape" && document.body.classList.contains("nav-open")) { close(); toggle?.focus(); }
            if (e.key === "Tab" && document.body.classList.contains("nav-open")) {
                const focusable = [toggle, ...sidebar.querySelectorAll("a,button")]; const index = focusable.indexOf(document.activeElement); e.preventDefault(); focusable[(index + (e.shiftKey ? -1 : 1) + focusable.length) % focusable.length].focus();
            }
        });
        const breakpoint = window.matchMedia("(max-width: 900px)"); const update = () => { close(); if (sidebar) sidebar.inert = breakpoint.matches; }; breakpoint.addEventListener("change", update); update();
        document.getElementById("menu-sair")?.addEventListener("click", () => { CkgdAPI.encerrarSessao(); window.location.href = "login.html"; });
        document.getElementById("menu-suporte")?.addEventListener("click", () => { if (document.body.classList.contains("nav-open")) { close(); toggle?.focus(); } if (typeof CkgdSuporte !== "undefined") CkgdSuporte.abrirModal(); });
        this.updateCompareIndicator();
    },
    initPasswords() {
        document.querySelectorAll("[data-password-toggle]").forEach(button => button.addEventListener("click", () => { const input = document.getElementById(button.dataset.passwordToggle); if (!input) return; const visible = input.type === "password"; input.type = visible ? "text" : "password"; button.textContent = visible ? "Ocultar" : "Mostrar"; button.setAttribute("aria-pressed", String(visible)); }));
    }
};
OasisUI.initPasswords();
if (document.body.classList.contains("app-shell")) OasisUI.initShell();
