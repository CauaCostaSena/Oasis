package com.ckgd.service;

import com.ckgd.dto.AlterarSenhaRequest;
import com.ckgd.dto.AtualizarEmpresaRequest;
import com.ckgd.dto.AuthResponse;
import com.ckgd.dto.CadastroEmpresaRequest;
import com.ckgd.dto.LoginRequest;
import com.ckgd.dto.RedefinirSenhaRequest;
import com.ckgd.entity.Empresa;
import com.ckgd.entity.PlanoDeAssinatura;
import com.ckgd.exception.BusinessException;
import com.ckgd.exception.ResourceNotFoundException;
import com.ckgd.repository.EmpresaRepository;
import com.ckgd.exception.FeatureUnavailableException;
import com.ckgd.security.JwtUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.Locale;

@Service
public class EmpresaService {

    private static final Set<String> TIPOS_IMAGEM_PERMITIDOS = Set.of("image/jpeg", "image/png");

    private final EmpresaRepository empresaRepository;
    private final PlanoGratuitoService planoGratuitoService;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;

    @Value("${ckgd.upload.dir:uploads}")
    private String uploadDir;

    public EmpresaService(EmpresaRepository empresaRepository,
                           PlanoGratuitoService planoGratuitoService,
                           PasswordEncoder passwordEncoder,
                           AuthenticationManager authenticationManager,
                           JwtUtil jwtUtil) {
        this.empresaRepository = empresaRepository;
        this.planoGratuitoService = planoGratuitoService;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
    }

    @Transactional
    public AuthResponse cadastrar(CadastroEmpresaRequest req) {
        if (empresaRepository.existsById(req.getCnpj())) {
            throw new BusinessException("Já existe uma empresa cadastrada com este CNPJ");
        }
        if (empresaRepository.existsByEmailIgnoreCase(normalizarEmail(req.getEmail()))) {
            throw new BusinessException("Já existe uma empresa cadastrada com este e-mail");
        }

        // O cliente nunca escolhe um plano no cadastro, mesmo enviando um idPlano legado.
        PlanoDeAssinatura plano = planoGratuitoService.buscar();

        Empresa empresa = new Empresa();
        empresa.setCnpj(req.getCnpj());
        empresa.setNomeEmpresa(req.getNomeEmpresa());
        empresa.setEmail(normalizarEmail(req.getEmail()));
        empresa.setSenha(passwordEncoder.encode(req.getSenha()));
        empresa.setPais(req.getPais());
        empresa.setEstado(req.getEstado());
        empresa.setCidade(req.getCidade());
        empresa.setBairro(req.getBairro());
        empresa.setEndereco(req.getEndereco());
        empresa.setPlano(plano);

        empresa = empresaRepository.save(empresa);

        String token = jwtUtil.gerarToken(empresa.getCnpj(), "EMPRESA");
        return new AuthResponse(token, empresa.getCnpj(), empresa.getNomeEmpresa(), empresa.getEmail());
    }

    public boolean existePorEmail(String email) {
        return empresaRepository.existsByEmailIgnoreCase(normalizarEmail(email));
    }

    public AuthResponse login(LoginRequest req) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(normalizarEmail(req.getEmail()), req.getSenha()));
        } catch (BadCredentialsException e) {
            throw new BadCredentialsException("E-mail ou senha incorretos");
        }

        Empresa empresa = empresaRepository.findByEmailIgnoreCase(normalizarEmail(req.getEmail()))
                .orElseThrow(() -> new ResourceNotFoundException("Empresa não encontrada"));

        String token = jwtUtil.gerarToken(empresa.getCnpj(), "EMPRESA");
        return new AuthResponse(token, empresa.getCnpj(), empresa.getNomeEmpresa(), empresa.getEmail());
    }

    public Empresa buscarPorCnpj(String cnpj) {
        return empresaRepository.findById(cnpj)
                .orElseThrow(() -> new ResourceNotFoundException("Empresa não encontrada"));
    }

    @Transactional
    public Empresa atualizarPerfil(String cnpj, AtualizarEmpresaRequest req) {
        Empresa empresa = buscarPorCnpj(cnpj);

        if (StringUtils.hasText(req.getNomeEmpresa())) {
            empresa.setNomeEmpresa(req.getNomeEmpresa().trim());
        }
        if (req.getTelefone() != null) {
            String telefone = req.getTelefone().trim();
            empresa.setTelefone(telefone.isEmpty() ? null : telefone);
        }

        return empresaRepository.save(empresa);
    }

    @Transactional
    public Empresa atualizarFoto(String cnpj, MultipartFile arquivo) {
        if (arquivo == null || arquivo.isEmpty()) {
            throw new BusinessException("Nenhum arquivo enviado");
        }
        if (!TIPOS_IMAGEM_PERMITIDOS.contains(arquivo.getContentType())) {
            throw new BusinessException("Formato de imagem inválido. Use JPEG ou PNG");
        }

        // O MIME declarado pelo cliente não comprova o conteúdo. Decodificar e
        // regravar remove conteúdo anexado e metadados enviados pelo cliente.
        java.awt.image.BufferedImage imagem;
        try (var entrada = javax.imageio.ImageIO.createImageInputStream(arquivo.getInputStream())) {
            var readers = javax.imageio.ImageIO.getImageReaders(entrada);
            if (!readers.hasNext()) throw new BusinessException("Envie uma imagem JPEG ou PNG válida.");
            var reader = readers.next();
            try {
                reader.setInput(entrada);
                String formato = reader.getFormatName();
                if (!(formato.equalsIgnoreCase("JPEG") || formato.equalsIgnoreCase("PNG")))
                    throw new BusinessException("Envie uma imagem JPEG ou PNG válida.");
                if ((long) reader.getWidth(0) * reader.getHeight(0) > 16_000_000)
                    throw new BusinessException("A imagem deve ter no máximo 16 milhões de pixels.");
                imagem = reader.read(0);
            } finally { reader.dispose(); }
        } catch (IOException ex) {
            throw new BusinessException("Não foi possível ler a imagem enviada.");
        }

        Empresa empresa = buscarPorCnpj(cnpj);

        String extensao = ".png";

        try {
            Path pastaDestino = Paths.get(uploadDir, "empresas");
            Files.createDirectories(pastaDestino);

            Path destino = pastaDestino.resolve(cnpj + extensao);
            Path temporario = Files.createTempFile(pastaDestino, "foto-", ".tmp");
            try {
                if (!javax.imageio.ImageIO.write(imagem, "png", temporario.toFile()))
                    throw new IOException("Codificador indisponível");
                Files.move(temporario, destino, StandardCopyOption.REPLACE_EXISTING);
            } finally { Files.deleteIfExists(temporario); }
        } catch (IOException e) {
            throw new UncheckedIOException("Falha ao salvar a foto de perfil", e);
        }

        empresa.setFotoUrl("/uploads/empresas/" + cnpj + extensao);
        return empresaRepository.save(empresa);
    }

    @Transactional
    public void alterarSenha(String cnpj, AlterarSenhaRequest req) {
        Empresa empresa = buscarPorCnpj(cnpj);

        if (!passwordEncoder.matches(req.getSenhaAtual(), empresa.getSenha())) {
            throw new BusinessException("Senha atual incorreta");
        }

        empresa.setSenha(passwordEncoder.encode(req.getNovaSenha()));
        empresaRepository.save(empresa);
    }

    public void redefinirSenha(RedefinirSenhaRequest req) {
        // CNPJ e e-mail públicos não comprovam posse da conta.
        throw new FeatureUnavailableException("A recuperação de senha está temporariamente indisponível. É necessário um canal de confirmação verificado.");
    }

    /** O chamador deve manter uma transação para proteger verificação e consumo de limites. */
    public Empresa buscarPorCnpjComLock(String cnpj) {
        return empresaRepository.findByCnpjForUpdate(cnpj)
                .orElseThrow(() -> new ResourceNotFoundException("Empresa não encontrada"));
    }

    private static String normalizarEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
