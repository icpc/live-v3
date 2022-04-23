package org.icpclive.cds.yandex.api

fun getResult(verdict: String) = when (verdict) {
    "OK" -> "OK"
    "WrongAnswer" -> "WA"
    "TimeLimitExceeded" -> "TL"
    "MemoryLimitExceeded" -> "ML"
    "RuntimeError" -> "RE"
    "IdlenessLimitExceeded" -> "IL"
    "PresentationError" -> "PE"
    "OutputLimitExceeded" -> "OL"
    "CompilationError" -> "CE"
    "Ignored" -> "IG"
    "PrecompileCheckFailed" -> "PC"

    // in testing
    "" -> ""

    // I think we don't need to show those:
    "SecurityViolation" -> "RE"
    // "AcceptedForTesting" -> "AC" // impossible on real contest
    // "ManualInspection" -> "MI" // impossible on real contest
    "Crash" -> "" // mark it as “judging” before some (human) judge resolves it

    else -> "??"
}