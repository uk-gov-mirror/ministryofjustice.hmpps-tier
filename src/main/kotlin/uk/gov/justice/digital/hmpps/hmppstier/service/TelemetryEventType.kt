package uk.gov.justice.digital.hmpps.hmppstier.service

enum class TelemetryEventType(val eventName: String) {
  TIER_CHANGED("TierChanged"),
  TIER_UNCHANGED("TierUnchanged");
}
