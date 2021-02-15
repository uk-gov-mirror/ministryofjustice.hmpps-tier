package uk.gov.justice.digital.hmpps.hmppstier.service

import com.fasterxml.jackson.databind.ObjectMapper
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.fail
import uk.gov.justice.digital.hmpps.hmppstier.config.ObjectMapperConfiguration
import uk.gov.justice.digital.hmpps.hmppstier.controller.TierCalculationRequiredEventListener
import uk.gov.justice.digital.hmpps.hmppstier.domain.TierLevel
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.ChangeLevel.TWO
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.ProtectLevel.B
import uk.gov.justice.digital.hmpps.hmppstier.dto.CalculationResultDto
import uk.gov.justice.digital.hmpps.hmppstier.dto.TierDto
import java.nio.file.Files
import java.nio.file.Paths

@ExtendWith(MockKExtension::class)
@DisplayName("Tier Calculation Event Listener tests")
class TierCalculationRequiredEventListenerTest {

  private val successUpdater: SuccessUpdater = mockk(relaxUnitFun = true)
  private val tierCalculationService: TierCalculationService = mockk(relaxUnitFun = true)
  private val objectMapper: ObjectMapper = ObjectMapperConfiguration().objectMapper()

  private val protect = TierLevel(B, 0)
  private val change = TierLevel(TWO, 0)
  private val crn = "X373878"

  @BeforeEach
  fun resetAllMocks() {
    clearMocks(tierCalculationService)
  }

  @AfterEach
  fun confirmVerified() {
    io.mockk.confirmVerified(tierCalculationService)
  }

  @Nested
  @DisplayName("updater is enabled")
  inner class UpdaterEnabled {
    private val updaterEnabledListener: TierCalculationRequiredEventListener =
      TierCalculationRequiredEventListener(objectMapper, tierCalculationService, successUpdater, true)

    @Test
    fun `should not call community-api update tier on failure`() {
      val validMessage: String =
        Files.readString(Paths.get("src/test/resources/fixtures/sqs/tier-calculation-event.json"))

      every { tierCalculationService.calculateTierForCrn(crn) } throws IllegalArgumentException("Oops")

      try {
        updaterEnabledListener.listen(validMessage)
        fail("Should have thrown an exception")
      } catch (e: IllegalArgumentException) {
        verify { tierCalculationService.calculateTierForCrn(crn) }
        verify(exactly = 0) { successUpdater.update(any(), any()) }
      }
    }

    @Test
    fun `should not call community-api update tier if tier has been calculated before and is unchanged`() {
      val validMessage: String =
        Files.readString(Paths.get("src/test/resources/fixtures/sqs/tier-calculation-event.json"))

      val calculationResult = CalculationResultDto(
        TierDto(
          protect.tier,
          protect.points,
          change.tier,
          change.points
        ),
        false
      )
      every { tierCalculationService.calculateTierForCrn(crn) } returns
        calculationResult

      updaterEnabledListener.listen(validMessage)

      verify { tierCalculationService.calculateTierForCrn(crn) }
      verify(exactly = 0) { successUpdater.update(calculationResult.tierDto, crn) }
    }

    @Test
    fun `should call community-api update tier if tier has changed`() {
      val validMessage: String =
        Files.readString(Paths.get("src/test/resources/fixtures/sqs/tier-calculation-event.json"))

      val calculationResult = CalculationResultDto(
        TierDto(
          protect.tier,
          protect.points,
          change.tier,
          change.points
        ),
        true
      )
      every { tierCalculationService.calculateTierForCrn(crn) } returns
        calculationResult

      updaterEnabledListener.listen(validMessage)

      verify { tierCalculationService.calculateTierForCrn(crn) }
      verify { successUpdater.update(calculationResult.tierDto, crn) }
    }
  }

  @Nested
  @DisplayName("updater is disabled")
  inner class UpdaterDisabled {
    private val noUpdateListener: TierCalculationRequiredEventListener =
      TierCalculationRequiredEventListener(objectMapper, tierCalculationService, successUpdater, false)
    @Test
    fun `should not call updater if tier has changed`() {
      val validMessage: String =
        Files.readString(Paths.get("src/test/resources/fixtures/sqs/tier-calculation-event.json")) // TODO use a helper

      val calculationResult = CalculationResultDto(
        TierDto(
          protect.tier,
          protect.points,
          change.tier,
          change.points
        ),
        true
      )
      every { tierCalculationService.calculateTierForCrn(crn) } returns
        calculationResult

      noUpdateListener.listen(validMessage)

      verify { tierCalculationService.calculateTierForCrn(crn) }
      verify(exactly = 0) { successUpdater.update(calculationResult.tierDto, crn) }
    }
  }
}
