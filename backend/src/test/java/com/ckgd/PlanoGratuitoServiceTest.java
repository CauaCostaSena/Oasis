package com.ckgd;

import com.ckgd.entity.PlanoDeAssinatura;
import com.ckgd.repository.PlanoDeAssinaturaRepository;
import com.ckgd.service.PlanoGratuitoService;
import com.ckgd.exception.BusinessException;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.List;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class PlanoGratuitoServiceTest {
    private PlanoDeAssinatura plano(int id,String nome,String valor) {
        var p=new PlanoDeAssinatura(); p.setIdPlano(id); p.setNomePlano(nome); p.setPrecoPlano(new BigDecimal(valor)); return p;
    }
    @Test void reconheceNomeLegadoSemEscolherEnterpriseDePrecoZero() {
        var repo=mock(PlanoDeAssinaturaRepository.class);
        var gratuito=plano(2,"Oásis Básico","0.00");
        when(repo.findAll()).thenReturn(List.of(plano(1,"Enterprise","0.00"),gratuito));
        assertThat(new PlanoGratuitoService(repo,"Free").buscar()).isSameAs(gratuito);
    }
    @Test void naoCadastraEmPlanoPagoOuInativoQuandoGratuitoIndisponivel() {
        var repo=mock(PlanoDeAssinaturaRepository.class);
        var inativo=plano(1,"Free","0.00"); inativo.setStatusPlano(PlanoDeAssinatura.StatusPlano.INATIVO);
        when(repo.findAll()).thenReturn(List.of(inativo,plano(2,"Básico","10.00"),plano(3,"Enterprise","0.00")));
        assertThatThrownBy(() -> new PlanoGratuitoService(repo,"Free").buscar()).isInstanceOf(BusinessException.class);
    }
}
