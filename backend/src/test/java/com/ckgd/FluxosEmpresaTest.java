package com.ckgd;

import com.ckgd.entity.*;
import com.ckgd.repository.*;
import com.ckgd.security.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import java.util.Map;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:oasis_test;MODE=MySQL;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver", "spring.datasource.username=sa",
    "spring.datasource.password=", "spring.jpa.hibernate.ddl-auto=create-drop",
    "ckgd.upload.dir=target/test-uploads",
    "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
    "ckgd.jwt.secret=chave-exclusiva-de-testes-nao-usar-em-producao-123456789"
})
@AutoConfigureMockMvc
@Transactional
class FluxosEmpresaTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired EmpresaRepository empresas;
    @Autowired CandidatoRepository candidatos;
    @Autowired EmpresaCandidatoRepository vinculos;
    @Autowired PlanoDeAssinaturaRepository planos;
    @Autowired PasswordEncoder encoder;
    @Autowired JwtUtil jwt;

    private PlanoDeAssinatura free() { return planos.findAll().stream().filter(p -> p.getNomePlano().equals("Free")).findFirst().orElseThrow(); }
    private Empresa empresa(String cnpj) {
        Empresa e = new Empresa(); e.setCnpj(cnpj); e.setNomeEmpresa("Empresa de teste");
        e.setEmail(cnpj + "@example.test"); e.setSenha(encoder.encode("Senha-teste-123")); e.setPlano(free());
        return empresas.saveAndFlush(e);
    }
    private String token(Empresa e) { return "Bearer " + jwt.gerarToken(e.getCnpj(), "EMPRESA"); }
    private Candidato candidato() {
        Candidato c = new Candidato(); c.setNodeId(42L); c.setUsername("teste"); c.setNomeCandidato("Teste");
        return candidatos.saveAndFlush(c);
    }

    @Test void cadastroHttpIgnoraPlanoPagoEAtribuiGratuitoSemFrontendEPermiteLogin() throws Exception {
        int pago = planos.findAll().stream().filter(p -> p.getPrecoPlano().signum() > 0).findFirst().orElseThrow().getIdPlano();
        var payload = Map.of("cnpj","12345678000199","nomeEmpresa","Empresa de teste","email","TESTE@example.test",
                "senha","Senha-teste-123","pais","Brasil","estado","Bahia","cidade","Salvador","bairro","Centro","endereco","Rua de teste","idPlano",pago);
        mvc.perform(post("/api/auth/cadastro").contentType("application/json").content(mapper.writeValueAsString(payload)))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.tipo").value("EMPRESA")).andExpect(jsonPath("$.token").isNotEmpty());
        Empresa criada = empresas.findById("12345678000199").orElseThrow();
        assertThat(criada.getPlano().getIdPlano()).isEqualTo(free().getIdPlano()).isNotEqualTo(pago);
        assertThat(criada.getPlano().getPrecoPlano()).isZero();
        assertThat(criada.getSenha()).isNotEqualTo("Senha-teste-123");
        assertThat(encoder.matches("Senha-teste-123",criada.getSenha())).isTrue();
        mvc.perform(post("/api/auth/login").contentType("application/json").content("{\"email\":\"teste@example.test\",\"senha\":\"Senha-teste-123\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.tipo").value("EMPRESA"));
        mvc.perform(post("/api/auth/login").contentType("application/json").content("{\"email\":\"teste@example.test\",\"senha\":\"incorreta\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test void favoritoEAvaliacaoSaoIndependentesEPrivadosEntreEmpresas() throws Exception {
        Empresa a=empresa("11111111000111"), b=empresa("22222222000122"); candidato();
        mvc.perform(put("/api/favoritos/42").header("Authorization",token(a)).contentType("application/json").content("{\"favorito\":true}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.dataAvaliacao").isEmpty());
        mvc.perform(put("/api/avaliacoes/42").header("Authorization",token(a)).contentType("application/json")
                .content("{\"nota\":5,\"comentario\":\"Interno\",\"privada\":false,\"cnpj\":\"22222222000122\",\"favorito\":false}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.privada").value(true)).andExpect(jsonPath("$.favorito").value(true));
        mvc.perform(get("/api/avaliacoes").header("Authorization",token(b))).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
        mvc.perform(get("/api/avaliacoes/42").header("Authorization",token(b))).andExpect(status().isNotFound());
        mvc.perform(delete("/api/favoritos/42").header("Authorization",token(a))).andExpect(status().isNoContent());
        mvc.perform(get("/api/avaliacoes/42").header("Authorization",token(a)))
                .andExpect(jsonPath("$.nota").value(5)).andExpect(jsonPath("$.comentario").value("Interno")).andExpect(jsonPath("$.favorito").value(false));
        mvc.perform(put("/api/avaliacoes/42").header("Authorization",token(a)).contentType("application/json").content("{\"nota\":null,\"comentario\":\"Somente comentário\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.nota").isEmpty());
    }

    @Test void validaEscalaELimiteSemCobrarFavorito() throws Exception {
        Empresa a=empresa("11111111000111"); Candidato c=candidato();
        a.getPlano().setLimiteAvaliacao(1); planos.saveAndFlush(a.getPlano());
        for(int nota : new int[]{0,6}) {
            mvc.perform(put("/api/avaliacoes/42").header("Authorization",token(a)).contentType("application/json").content("{\"nota\":"+nota+"}"))
                    .andExpect(status().isBadRequest());
        }
        mvc.perform(put("/api/avaliacoes/42").header("Authorization",token(a)).contentType("application/json").content("{\"nota\":1}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.favorito").value(false));
        mvc.perform(put("/api/avaliacoes/42").header("Authorization",token(a)).contentType("application/json").content("{\"nota\":4}"))
                .andExpect(status().isOk());
        Candidato segundo=new Candidato(); segundo.setNodeId(43L); segundo.setUsername("outro"); candidatos.saveAndFlush(segundo);
        mvc.perform(put("/api/avaliacoes/43").header("Authorization",token(a)).contentType("application/json").content("{\"nota\":3}"))
                .andExpect(status().isBadRequest());
        mvc.perform(put("/api/favoritos/43").header("Authorization",token(a)).contentType("application/json").content("{\"favorito\":true}"))
                .andExpect(status().isOk());
        assertThat(vinculos.contarAvaliacoes(a.getCnpj())).isEqualTo(1);
    }

    @Test void protegeRotasERecusaFluxosDeCandidatoERecuperacaoInsegura() throws Exception {
        mvc.perform(get("/api/favoritos")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/empresas/me").header("Authorization","Bearer invalido")).andExpect(status().isUnauthorized());
        Empresa e=empresa("11111111000111");
        mvc.perform(post("/api/auth/cadastro-candidato").header("Authorization",token(e)).contentType("application/json").content("{}"))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/candidatos/me").header("Authorization",token(e))).andExpect(status().isForbidden());
        mvc.perform(post("/api/auth/redefinir-senha").contentType("application/json")
                .content("{\"cnpj\":\"11111111000111\",\"email\":\"teste@example.test\",\"novaSenha\":\"Nova-senha-123\"}"))
                .andExpect(status().isServiceUnavailable());
        assertThat(encoder.matches("Senha-teste-123",empresas.findById(e.getCnpj()).orElseThrow().getSenha())).isTrue();
    }

    @Test void rejeitaArquivoFalsoERegravaImagemValida() throws Exception {
        Empresa e=empresa("11111111000111");
        var falso=new org.springframework.mock.web.MockMultipartFile("arquivo","foto.png","image/png","<script>alert(1)</script>".getBytes());
        mvc.perform(multipart("/api/empresas/me/foto").file(falso).header("Authorization",token(e))).andExpect(status().isBadRequest());
        var imagem=new java.awt.image.BufferedImage(2,2,java.awt.image.BufferedImage.TYPE_INT_RGB);
        var bytes=new java.io.ByteArrayOutputStream(); javax.imageio.ImageIO.write(imagem,"png",bytes);
        var valido=new org.springframework.mock.web.MockMultipartFile("arquivo","foto.png","image/png",bytes.toByteArray());
        mvc.perform(multipart("/api/empresas/me/foto").file(valido).header("Authorization",token(e)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.fotoUrl").value("/uploads/empresas/11111111000111.png"));
        assertThat(javax.imageio.ImageIO.read(new java.io.File("target/test-uploads/empresas/11111111000111.png"))).isNotNull();
    }

    @Test void rejeitaOrigemCorsNaoAutorizadaENotaFracionaria() throws Exception {
        mvc.perform(options("/api/auth/login").header("Origin","https://nao-autorizado.example").header("Access-Control-Request-Method","POST"))
                .andExpect(status().isForbidden());
        Empresa e=empresa("11111111000111"); candidato();
        mvc.perform(put("/api/avaliacoes/42").header("Authorization",token(e)).contentType("application/json").content("{\"nota\":1.5}"))
                .andExpect(status().isBadRequest());
    }
}
