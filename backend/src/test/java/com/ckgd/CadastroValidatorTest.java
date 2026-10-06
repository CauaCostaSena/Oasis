package com.ckgd;

import com.ckgd.exception.BusinessException;
import com.ckgd.validation.CadastroValidator;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CadastroValidatorTest {
    @Test
    void aceitaCnpjValidoERejeitaQuantidadeChecksumESequencia() {
        assertThat(CadastroValidator.validarCnpj("11222333000181")).isEqualTo("11222333000181");
        assertThatThrownBy(() -> CadastroValidator.validarCnpj("123")).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> CadastroValidator.validarCnpj("11111111111111")).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> CadastroValidator.validarCnpj("12345678000199")).isInstanceOf(BusinessException.class);
    }

    @Test
    void paisPrecisaExistirEEstadoBrasileiroPrecisaSerValido() {
        assertThat(CadastroValidator.normalizarPais("BR")).isEqualTo("Brasil");
        assertThat(CadastroValidator.normalizarPais("Brasil")).isEqualTo("Brasil");
        assertThat(CadastroValidator.normalizarEstado("Brasil", "BA")).isEqualTo("Bahia");
        assertThat(CadastroValidator.normalizarEstado("Brasil", "Bahia")).isEqualTo("Bahia");
        assertThatThrownBy(() -> CadastroValidator.normalizarPais("NBEWFCKHIERCB")).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> CadastroValidator.normalizarEstado("Brasil", "Estado inventado")).isInstanceOf(BusinessException.class);
    }
}
