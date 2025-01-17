package uk.gov.justice.digital.hmpps.hmppstier.integration

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS
import uk.gov.justice.digital.hmpps.hmppstier.integration.setup.MockedEndpointsTestBase
import uk.gov.justice.digital.hmpps.hmppstier.integration.setup.registrationsResponse

@TestInstance(PER_CLASS)
class NeedsTest : MockedEndpointsTestBase() {

  @Test
  fun `severe needs 18 points plus 2 OGRS make change level 3`() {
    val crn = "X333445"
    setupSCCustodialSentence(crn)
    setupRegistrations(registrationsResponse(), crn)
    restOfSetupWithMaleOffenderAndSevereNeeds(crn)
    calculateTierFor(crn)
    expectTierCalculation("A3")
  }
}
