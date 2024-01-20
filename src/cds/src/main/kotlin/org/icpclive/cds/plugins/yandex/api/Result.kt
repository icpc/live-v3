package org.icpclive.cds.plugins.yandex.api

import org.icpclive.api.Verdict

internal fun getResult(verdict: String) = when (verdict) {
    "OK" -> Verdict.Accepted
    "WrongAnswer" -> Verdict.WrongAnswer
    "TimeLimitExceeded" -> Verdict.TimeLimitExceeded
    "MemoryLimitExceeded" -> Verdict.MemoryLimitExceeded
    "RuntimeError" -> Verdict.RuntimeError
    "IdlenessLimitExceeded" -> Verdict.IdlenessLimitExceeded
    "PresentationError" -> Verdict.PresentationError
    "OutputLimitExceeded" -> Verdict.OutputLimitExceeded
    "CompilationError" -> Verdict.CompilationError
    "Ignored" -> Verdict.Ignored
    "PrecompileCheckFailed" -> Verdict.CompilationError

    // in testing
    "" -> null

    // I think we don't need to show those:
    "SecurityViolation" -> Verdict.RuntimeError
    // "AcceptedForTesting" -> "AC" // impossible on real contest
    // "ManualInspection" -> "MI" // impossible on real contest
    "Crash" -> null // mark it as “judging” before some (human) judge resolves it

    else -> null
}