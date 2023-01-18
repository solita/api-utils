package fi.solita.utils.api.html;

import static org.junit.Assert.*;

import org.junit.Test;

import fi.solita.utils.api.format.HtmlConversionService;

public class UITest {

    @Test
    public void calculateHash() {
        assertEquals("sha256-1shFwz2+GoGhS/eb9QvxLY8Vk/JJlJXyPwkJg3owzl8=", UI.calculateHash(HtmlConversionService.scripts2()));
    }
}
