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
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.fail
import uk.gov.justice.digital.hmpps.hmppstier.config.ObjectMapperConfiguration
import uk.gov.justice.digital.hmpps.hmppstier.domain.TierLevel
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.ChangeLevel
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.ChangeLevel.TWO
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.ProtectLevel.B
import uk.gov.justice.digital.hmpps.hmppstier.dto.TierDto
import uk.gov.justice.digital.hmpps.hmppstier.jpa.entity.TierCalculationEntity
import uk.gov.justice.digital.hmpps.hmppstier.jpa.entity.TierCalculationResultEntity
import java.nio.file.Files
import java.nio.file.Paths
import java.time.LocalDateTime

@ExtendWith(MockKExtension::class)
@DisplayName("Tier Calculation Event Listener tests")
class TierCalculationRequiredEventListenerTest {

  private val successUpdater: SuccessUpdater = mockk(relaxUnitFun = true)
  private val tierCalculationService: TierCalculationService = mockk(relaxUnitFun = true)
  private val objectMapper: ObjectMapper = ObjectMapperConfiguration().objectMapper()

  private val listener: TierCalculationRequiredEventListener =
    TierCalculationRequiredEventListener(objectMapper, tierCalculationService, successUpdater)

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

  @Test
  fun `should call community-api update tier on success`() {
    val validMessage: String =
      Files.readString(Paths.get("src/test/resources/fixtures/sqs/tier-calculation-event.json"))

    val calculationResult = TierDto(
      protect.tier,
      protect.points,
      change.tier,
      change.points
    )

    val result = TierCalculationResultEntity(TierLevel(protect.tier, 99), TierLevel(ChangeLevel.ONE, 88))
    val previousCalculation = TierCalculationEntity(crn = crn, created = LocalDateTime.now(), data = result)
    every { tierCalculationService.getTierCalculation(crn) } returns
      previousCalculation

    every { tierCalculationService.calculateTierForCrn(crn) } returns
      calculationResult
    every { successUpdater.update(calculationResult) } returns Unit

    listener.listen(validMessage)
    verify { tierCalculationService.getTierCalculation(crn) }
    verify { tierCalculationService.calculateTierForCrn(crn) }
    verify { successUpdater.update(calculationResult) }
  }

  @Test
  fun `should not call community-api update tier on failure`() {
    val validMessage: String =
      Files.readString(Paths.get("src/test/resources/fixtures/sqs/tier-calculation-event.json"))
    val result = TierCalculationResultEntity(TierLevel(protect.tier, 99), TierLevel(ChangeLevel.ONE, 88))
    val previousCalculation = TierCalculationEntity(crn = crn, created = LocalDateTime.now(), data = result)
    every { tierCalculationService.getTierCalculation(crn) } returns
      previousCalculation
    every { tierCalculationService.calculateTierForCrn(crn) } throws IllegalArgumentException("Oops")

    try {
      listener.listen(validMessage)
      fail("Should have thrown an exception")
    } catch (e: IllegalArgumentException) {
      verify { tierCalculationService.getTierCalculation(crn) }

      verify { tierCalculationService.calculateTierForCrn(crn) }
      verify(exactly = 0) { successUpdater.update(any()) }
    }
  }

  @Test
  fun `should not call community-api update tier if tier has been calculated before and is unchanged`() {
    val validMessage: String =
      Files.readString(Paths.get("src/test/resources/fixtures/sqs/tier-calculation-event.json"))

    val calculationResult = TierDto(
      protect.tier,
      protect.points,
      change.tier,
      change.points
    )
    every { tierCalculationService.calculateTierForCrn(crn) } returns
      calculationResult

    val result = TierCalculationResultEntity(TierLevel(protect.tier, protect.points), TierLevel(change.tier, change.points))
    val previousCalculation = TierCalculationEntity(crn = crn, created = LocalDateTime.now(), data = result)
    every { tierCalculationService.getTierCalculation(crn) } returns previousCalculation
    listener.listen(validMessage)
    verify { tierCalculationService.getTierCalculation(crn) }
    verify { tierCalculationService.calculateTierForCrn(crn) }
    verify(exactly = 0) { successUpdater.update(calculationResult) }
  }
}
