package uk.gov.justice.digital.hmpps.hmppstier.domain.enums

enum class TierMatchCriteria {
  RSR_USED_OVER_ROSH,
  ROSH_USED_OVER_RSR,
  RSR_ROSH_EQUAL,
  RSR_IN_TIER_B,
  RSR_IN_TIER_C,
  RSR_NO_MATCH,
  ROSH_VERY_HIGH,
  ROSH_HIGH,
  ROSH_MEDIUM,
  ROSH_NO_MATCH,
  MAPPA_LEVEL_2_OR_3,
  MAPPA_LEVEL_1,
  MAPPA_NO_MATCH,
  INCLUDED_COMPLEXITY_FACTORS,
  NO_COMPLEXITY_FACTORS,
  INCLUDED_ORGS,
  NO_ORGS,
  INCLUDED_OASYS_NEEDS,
  NO_OASYS_NEEDS,
}