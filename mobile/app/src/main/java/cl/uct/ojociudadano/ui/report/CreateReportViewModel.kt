package cl.uct.ojociudadano.ui.report

import androidx.lifecycle.ViewModel

class CreateReportViewModel(
    private val formValidator: CreateReportFormValidator = CreateReportFormValidator()
) : ViewModel() {

    fun validateForm(state: CreateReportFormState): CreateReportFormState {
        return formValidator.validate(state)
    }
}
