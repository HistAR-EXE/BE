package com.histar.be.usercreation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.histar.be.common.exception.BusinessRuleException;
import org.junit.jupiter.api.Test;

class HeritageCreationContextTest {

    @Test
    void acceptsPublishedHeritageContext() {
        HeritageCreationContext context = HeritageCreationContext.validate(
                "heritage", "cu-chi", "ST01", "st01-can-bo-lien-lac-nam", 1968, "station");

        assertEquals("heritage", context.kind());
        assertEquals("ST01", context.stationCode());
    }

    @Test
    void rejectsHeritageWithout1968Era() {
        assertThrows(BusinessRuleException.class, () -> HeritageCreationContext.validate(
                "heritage", "cu-chi", "ST01", "st01-can-bo-lien-lac-nam", 1975, "station"));
    }

    @Test
    void rejectsRoleFromAnotherStation() {
        assertThrows(BusinessRuleException.class, () -> HeritageCreationContext.validate(
                "heritage", "cu-chi", "ST02", "st01-can-bo-lien-lac-nam", 1968, "station"));
    }

    @Test
    void rejectsHeritageRoleAttachedToOriginalPhoto() {
        assertThrows(BusinessRuleException.class, () -> HeritageCreationContext.validate(
                "original", "cu-chi", "ST01", "st01-can-bo-lien-lac-nam", null, "station"));
    }
}
