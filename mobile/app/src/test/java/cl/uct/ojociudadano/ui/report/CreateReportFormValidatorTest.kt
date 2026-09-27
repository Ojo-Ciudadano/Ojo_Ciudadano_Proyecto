package cl.uct.ojociudadano.ui.report

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CreateReportFormValidatorTest {

    private val validator = CreateReportFormValidator()

    @Test
    fun `category is required when no selection exists`() {
        val validatedState = validator.validate(CreateReportFormState())

        assertEquals("La categoría es obligatoria", validatedState.categoryError)
    }

    @Test
    fun `selected category satisfies required validation`() {
        val validatedState = validator.validate(
            CreateReportFormState(
                selectedCategory = TemporaryReportCategories.options.first()
            )
        )

        assertNull(validatedState.categoryError)
    }
}
