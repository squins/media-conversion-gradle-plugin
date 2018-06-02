package com.squins.media.conversion.commandline

import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class VariantPropertyTest {

    @Test
    fun returnsNullIfPropertyIsNotSpecifiedAndIsOptional() {
        val name = VariantProperty("name", false)

        assertNull(name.resolve(emptyMap(), "/some/file.svg", "bogus"))
    }
}