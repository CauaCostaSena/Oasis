# Cobertura do prompt anexado

Matriz atualizada após a evolução visual e funcional iniciada em 30/09/2026.

| Itens do prompt | Situação atual |
|---|---|
| 1–6: escopo, empresa/candidato, gratuito, stack, auditoria | Implementado e preservado. Somente empresas autenticam; cadastro gratuito ocorre no backend; stack Java + HTML/CSS/JS mantida. |
| 7–10: limpeza, organização, conta candidato, marca | Conta de candidato removida; marca visível Oásis; artefatos históricos não fazem parte da árvore ativa. Identificadores técnicos CKGD continuam por compatibilidade. |
| 11–17: GitHub, IDs, linguagens, filtros, erros | Tratamento de erros/cache já existente preservado; linguagem por amostra; localização livre; sem inferência de idade/senioridade. Migração física de IDs ainda pendente. |
| 18–21: XSS, JWT, CORS, exceções | DOM seguro, segredo externo, CORS explícito e erros padronizados preservados. Estratégia de revogação de JWT continua futura. |
| 22–25: favoritos, avaliações, planos, limites | Favoritos separados; avaliações privadas; nota 1–5 mantida conforme decisão registrada no projeto; limites de busca/avaliação existentes. Limite de comparação não é consumido porque a regra permanece pendente. |
| 26–28: banco, rotinas, recuperação | DECIMAL mantido; rotinas previstas presentes; recuperação insegura desativada. Token de recuperação por canal verificado ainda pendente. |
| 29–30: testes e README | Testes existentes preservados; README atualizado; CI adicionado para Maven e verificação do frontend. |
| 31–46: referências, design system, landing, auth | Landing pública, hero, ticker, scroll reveal, FAQ, dark auth, tokens e identidade Oásis implementados sem copiar infraestrutura Webflow. |
| 47–59: app, sidebar, busca, filtros, loaders | Shell autenticado completo, sidebar, dashboard, busca, filtros mobile, skeletons e loader de filtro implementados. Loader global reutilizável disponível. |
| 60–64: perfil, repos, favoritos, comparação, Insights | Perfil/repositórios/favoritos mantidos; comparação funcional de até 3 perfis e Insights privados com dados reais implementados. |
| 65–69: assinatura e configurações | Página dedicada de assinatura consulta backend e destaca plano atual; alteração permanece explicitamente indisponível até regras comerciais. Perfil da empresa e configurações conectados. |
| 70–85: responsividade, foco, feedback, modais, performance | Breakpoints, drawer, foco, toast, modal, reduced-motion, transições e componentes compartilhados aplicados. Validação visual final em hardware real segue pendente. |
| 86–97: fases, decisões, validação e relatório | Trabalho fatiado, relatório de etapa adicionado e backlog explícito. CI faz checagens automatizadas; MySQL/GitHub ao vivo e decisões comerciais seguem pendentes. |
