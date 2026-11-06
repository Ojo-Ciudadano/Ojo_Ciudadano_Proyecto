package cl.uct.ojociudadano.ui.report

import cl.uct.ojociudadano.data.model.Category
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CreateReportFormValidatorTest {

    private val validator = CreateReportFormValidator()

    @Test
    fun `each location reference field is required and whitespace does not satisfy the contract`() {
        val valid = CreateReportFormState(referenceAddress = "Plaza", commune = "Temuco", region = "Araucanía", sector = "Centro")
        assertNull(validator.validate(valid).locationDetailsError)
        for (invalid in listOf(valid.copy(referenceAddress = " "), valid.copy(commune = ""),
            valid.copy(region = ""), valid.copy(sector = ""))) {
            assertEquals("Completa la dirección de referencia, comuna, región y sector", validator.validate(invalid).locationDetailsError)
        }
    }

    @Test
    fun `category is required when no selection exists`() {
        val validatedState = validator.validate(CreateReportFormState())

        assertEquals("La categoría es obligatoria", validatedState.categoryError)
    }

    @Test
    fun `selected category satisfies required validation`() {
        val validatedState = validator.validate(
            CreateReportFormState(
                selectedCategory = sampleCategory
            )
        )

        assertNull(validatedState.categoryError)
    }
}

private val sampleCategory = Category(
    id = "category-id",
    name = "Alumbrado público",
    description = "",
    basePriority = 0,
    isCriticalEmergency = false,
    officialContactChannel = null
)
