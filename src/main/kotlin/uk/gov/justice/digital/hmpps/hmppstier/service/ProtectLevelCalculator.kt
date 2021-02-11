package uk.gov.justice.digital.hmpps.hmppstier.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppstier.domain.TierLevel
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.AssessmentComplexityFactor
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.ComplexityFactor
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.Mappa
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.ProtectLevel
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.Rosh
import uk.gov.justice.digital.hmpps.hmppstier.domain.enums.RsrThresholds

@Service
class ProtectLevelCalculator(
  private val communityApiDataService: CommunityApiDataService,
  private val assessmentApiDataService: AssessmentApiDataService
) {
  fun calculateProtectLevel(crn: String): TierLevel<ProtectLevel> {
    val riskPoints = getRiskPoints(crn)
    val mappaPoints = getMappaPoints(crn)
    val complexityPoints = getComplexityPoints(crn)

    val totalPoints = riskPoints + mappaPoints + complexityPoints
    val tier = when {
      totalPoints >= 30 -> ProtectLevel.A
      totalPoints in 20..29 -> ProtectLevel.B
      totalPoints in 10..19 -> ProtectLevel.C
      else -> ProtectLevel.D
    }

    return TierLevel(tier, totalPoints)
  }

  private fun getRiskPoints(crn: String): Int {
    return maxOf(getRsrPoints(crn), getRoshPoints(crn))
  }

  private fun getRsrPoints(crn: String): Int {
    return communityApiDataService.getRSR(crn).let {
      when {
        it != null && it >= RsrThresholds.TIER_B_RSR.num -> 20
        it != null && it >= RsrThresholds.TIER_C_RSR.num -> 10
        else -> 0
      }
    }
  }

  private fun getRoshPoints(crn: String): Int {
    return communityApiDataService.getRosh(crn).let {
      when (it) {
        Rosh.VERY_HIGH -> 30
        Rosh.HIGH -> 20
        Rosh.MEDIUM -> 10
        else -> 0
      }
    }
  }

  private fun getMappaPoints(crn: String): Int {
    return when (communityApiDataService.getMappa(crn)) {
      Mappa.M2, Mappa.M3 -> 30
      Mappa.M1 -> 5
      else -> 0
    }
  }

  private fun getComplexityPoints(crn: String): Int {
    val complexityFactors = communityApiDataService.getComplexityFactors(crn).distinct().filter {
      // Filter out IOM it is not used in this calculation but is used in the change level calculation
      it != ComplexityFactor.IOM_NOMINAL
    }

    return complexityFactors.count().times(2).let {
      when {
        communityApiDataService.isFemaleOffender(crn) -> {
          it + getAssessmentComplexityPoints(crn) + getBreachRecallComplexityPoints(crn)
        }
        else -> it
      }
    }
  }

  private fun getAssessmentComplexityPoints(crn: String): Int {
    return assessmentApiDataService.getAssessmentComplexityAnswers(crn).let {
      val parenting = when {
        isYes(it[AssessmentComplexityFactor.PARENTING_RESPONSIBILITIES]) -> 2
        else -> 0
      }
      // We dont take the cumulative score, just '1' if at least one of these two is present
      val selfControl = when {
        isAnswered(it[AssessmentComplexityFactor.IMPULSIVITY]) || isAnswered(it[AssessmentComplexityFactor.TEMPER_CONTROL]) -> 2
        else -> 0
      }
      parenting.plus(selfControl)
    }
  }

  private fun getBreachRecallComplexityPoints(crn: String): Int {
    return when {
      communityApiDataService.hasBreachedConvictions(crn) -> 2
      else -> 0
    }
  }

  private fun isYes(value: String?): Boolean {
    return value != null && (value.equals("YES", true) || value.equals("Y", true))
  }

  private fun isAnswered(value: String?): Boolean {
    return value != null && value.toInt() > 0
  }
}