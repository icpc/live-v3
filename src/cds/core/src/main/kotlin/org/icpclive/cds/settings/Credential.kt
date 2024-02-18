package org.icpclive.cds.settings

public class Credential(public val displayValue: String, public val value: String) {
    override fun toString(): String = displayValue
}