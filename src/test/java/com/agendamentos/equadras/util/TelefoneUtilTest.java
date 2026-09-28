package com.agendamentos.equadras.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class TelefoneUtilTest {

    @Test
    void removeMascara() {
        assertEquals("17997785256", TelefoneUtil.normalizar("(17) 99778-5256"));
    }

    @Test
    void removeDdiDoBrasil() {
        assertEquals("17997785256", TelefoneUtil.normalizar("+55 17 99778-5256"));
    }

    @Test
    void mantemFixoDe10Digitos() {
        assertEquals("1733221100", TelefoneUtil.normalizar("(17) 3322-1100"));
    }

    @Test
    void nuloOuVazioViraNulo() {
        assertNull(TelefoneUtil.normalizar(null));
        assertNull(TelefoneUtil.normalizar("  "));
    }
}
