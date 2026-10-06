package com.ckgd.validation;

import com.ckgd.exception.BusinessException;

import java.text.Normalizer;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

public final class CadastroValidator {
    private static final Locale PT_BR = Locale.forLanguageTag("pt-BR");
    private static final Pattern COMBINING = Pattern.compile("\\p{M}+");
    private static final Pattern LOCALIDADE = Pattern.compile("[\\p{L}\\p{M}0-9 .,'’\\-/]{2,60}");
    private static final Map<String, String> PAISES = criarPaises();
    private static final Map<String, String> ESTADOS_BRASIL = criarEstadosBrasil();

    private CadastroValidator() {}

    public static String validarCnpj(String valor) {
        if (valor == null || !valor.matches("\\d{14}")) {
            throw new BusinessException("CNPJ deve conter exatamente 14 dígitos numéricos");
        }
        if (valor.chars().allMatch(ch -> ch == valor.charAt(0))) {
            throw new BusinessException("Informe um CNPJ válido");
        }
        if (digito(valor, 12) != valor.charAt(12) - '0' || digito(valor, 13) != valor.charAt(13) - '0') {
            throw new BusinessException("Informe um CNPJ válido");
        }
        return valor;
    }

    private static int digito(String cnpj, int tamanho) {
        int soma = 0;
        int peso = tamanho - 7;
        for (int i = 0; i < tamanho; i++) {
            soma += (cnpj.charAt(i) - '0') * peso--;
            if (peso < 2) peso = 9;
        }
        int resto = soma % 11;
        return resto < 2 ? 0 : 11 - resto;
    }

    public static String normalizarPais(String valor) {
        String pais = PAISES.get(chave(valor));
        if (pais == null) throw new BusinessException("Selecione um país válido");
        return pais;
    }

    public static String normalizarEstado(String paisCanonico, String valor) {
        if ("Brasil".equalsIgnoreCase(paisCanonico)) {
            String estado = ESTADOS_BRASIL.get(chave(valor));
            if (estado == null) throw new BusinessException("Selecione um estado brasileiro válido");
            return estado;
        }
        return normalizarLocalidade(valor, "Estado / província / região");
    }

    public static String normalizarLocalidade(String valor, String campo) {
        String texto = limparEspacos(valor);
        if (!LOCALIDADE.matcher(texto).matches() || texto.chars().noneMatch(Character::isLetter)) {
            throw new BusinessException(campo + " inválido(a)");
        }
        return texto;
    }

    public static String normalizarTexto(String valor, String campo, int minimo, int maximo) {
        String texto = limparEspacos(valor);
        if (texto.length() < minimo || texto.length() > maximo) {
            throw new BusinessException(campo + " deve ter entre " + minimo + " e " + maximo + " caracteres");
        }
        return texto;
    }

    private static String limparEspacos(String valor) {
        if (valor == null) return "";
        return valor.trim().replaceAll("\\s+", " ");
    }

    private static String chave(String valor) {
        String texto = limparEspacos(valor);
        String semAcento = COMBINING.matcher(Normalizer.normalize(texto, Normalizer.Form.NFD)).replaceAll("");
        return semAcento.toLowerCase(Locale.ROOT);
    }

    private static Map<String, String> criarPaises() {
        Map<String, String> mapa = new HashMap<>();
        for (String codigo : Locale.getISOCountries()) {
            Locale localePais = new Locale("", codigo);
            String pt = localePais.getDisplayCountry(PT_BR);
            String en = localePais.getDisplayCountry(Locale.ENGLISH);
            if (pt == null || pt.isBlank()) pt = codigo;
            mapa.put(chave(codigo), pt);
            mapa.put(chave(pt), pt);
            mapa.put(chave(en), pt);
        }
        mapa.put(chave("Brasil"), "Brasil");
        mapa.put(chave("Brazil"), "Brasil");
        mapa.put(chave("BR"), "Brasil");
        return Map.copyOf(mapa);
    }

    private static Map<String, String> criarEstadosBrasil() {
        String[][] estados = {
                {"AC","Acre"},{"AL","Alagoas"},{"AP","Amapá"},{"AM","Amazonas"},{"BA","Bahia"},{"CE","Ceará"},
                {"DF","Distrito Federal"},{"ES","Espírito Santo"},{"GO","Goiás"},{"MA","Maranhão"},{"MT","Mato Grosso"},
                {"MS","Mato Grosso do Sul"},{"MG","Minas Gerais"},{"PA","Pará"},{"PB","Paraíba"},{"PR","Paraná"},
                {"PE","Pernambuco"},{"PI","Piauí"},{"RJ","Rio de Janeiro"},{"RN","Rio Grande do Norte"},
                {"RS","Rio Grande do Sul"},{"RO","Rondônia"},{"RR","Roraima"},{"SC","Santa Catarina"},{"SP","São Paulo"},
                {"SE","Sergipe"},{"TO","Tocantins"}
        };
        Map<String, String> mapa = new HashMap<>();
        for (String[] estado : estados) {
            mapa.put(chave(estado[0]), estado[1]);
            mapa.put(chave(estado[1]), estado[1]);
        }
        return Map.copyOf(mapa);
    }
}
