package uk.gov.justice.digital.hmpps.hmppstier.service

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppstier.client.CommunityApiClient
import uk.gov.justice.digital.hmpps.hmppstier.client.Conviction
import uk.gov.justice.digital.hmpps.hmppstier.client.Requirement
import uk.gov.justice.digital.hmpps.hmppstier.client.Sentence

@Service
class MandateForChange(
  private val communityApiClient: CommunityApiClient
) {
  fun hasNoMandate(crn: String, convictions: Collection<Conviction>): Boolean =
    convictions
      .filter { it.sentence.terminationDate == null }
      .let { activeConvictions ->
        activeConvictions.none {
          (isCustodial(it.sentence) || hasNonRestrictiveRequirements(crn, it.convictionId))
        }
      }.also { log.debug("Has no mandate for change: $it") }

  private fun isCustodial(sentence: Sentence) =
    sentence.sentenceType.code in custodialSentences

  private fun hasNonRestrictiveRequirements(crn: String, convictionId: Long): Boolean =
    communityApiClient.getRequirements(crn, convictionId)
      .filter { excludeUnpaidWork(it) }
      .any { it.restrictive != true }
      .also { log.debug("Has non-restrictive requirements: $it") }

  private fun excludeUnpaidWork(it: Requirement) =
    it.requirementTypeMainCategory?.code != "W"

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
    private val custodialSentences = arrayOf("NC", "SC")
  }
}
