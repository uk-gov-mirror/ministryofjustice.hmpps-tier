package uk.gov.justice.digital.hmpps.hmppstier.domain.enums

enum class AssessmentComplexityFactor(val answerCode: String) {

  IMPULSIVITY("11.2"),
  TEMPER_CONTROL("11.4"),
  PARENTING_RESPONSIBILITIES("13.3 - F");

  companion object {
    fun from(value: String?): AssessmentComplexityFactor? {
      return values()
        .firstOrNull { code -> code.answerCode.equals(value, true) }
    }
  }
}