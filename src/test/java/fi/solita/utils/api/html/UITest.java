package fi.solita.utils.api.html;

import static org.junit.Assert.*;

import org.junit.Test;

import fi.solita.utils.api.format.HtmlConversionService;

public class UITest {

    @Test
    public void calculateHash() {
        // just tests hash calculation. Replace hash here when you modify the script
        assertEquals("sha256-hrTchaIJErAKAcJc7iW7pHe/ms/pPBb4ba18UI1eRlU=", UI.calculateHash(HtmlConversionService.scripts2()));
    }
}
