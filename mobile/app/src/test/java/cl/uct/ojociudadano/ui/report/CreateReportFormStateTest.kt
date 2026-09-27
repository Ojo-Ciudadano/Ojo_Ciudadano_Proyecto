package cl.uct.ojociudadano.ui.report

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CreateReportFormStateTest {

    @Test
    fun `updating title clears only its previous error`() {
        val state = CreateReportFormState(
            title = "",
            description = "",
            titleError = "El título es obligatorio",
            descriptionError = "La descripción es obligatoria",
            categoryError = "La categoría es obligatoria"
        )

        val updatedState = state.updateTitle("Poste dañado")

        assertEquals("Poste dañado", updatedState.title)
        assertNull(updatedState.titleError)
        assertEquals(state.description, updatedState.description)
        assertEquals(state.descriptionError, updatedState.descriptionError)
        assertEquals(state.categoryError, updatedState.categoryError)
    }

    @Test
    fun `updating description clears only its previous error`() {
        val state = CreateReportFormState(
            title = "Poste dañado",
            description = "",
            titleError = "El título es obligatorio",
            descriptionError = "La descripción es obligatoria",
            categoryError = "La categoría es obligatoria"
        )

        val updatedState = state.updateDescription("El poste bloquea la vereda")

        assertEquals("El poste bloquea la vereda", updatedState.description)
        assertNull(updatedState.descriptionError)
        assertEquals(state.title, updatedState.title)
        assertEquals(state.titleError, updatedState.titleError)
        assertEquals(state.categoryError, updatedState.categoryError)
    }

    @Test
    fun `updating category stores selection and clears only its previous error`() {
        val category = TemporaryReportCategories.options.first()
        val state = CreateReportFormState(
            title = "Poste dañado",
            description = "El poste bloquea la vereda",
            titleError = "El título es obligatorio",
            descriptionError = "La descripción es obligatoria",
            categoryError = "La categoría es obligatoria"
        )

        val updatedState = state.updateCategory(category)

        assertEquals(category, updatedState.selectedCategory)
        assertNull(updatedState.categoryError)
        assertEquals(state.titleError, updatedState.titleError)
        assertEquals(state.descriptionError, updatedState.descriptionError)
    }
}
