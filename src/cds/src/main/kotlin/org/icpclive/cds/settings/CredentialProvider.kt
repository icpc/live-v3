package org.icpclive.cds.settings

public fun interface CredentialProvider {
    public operator fun get(s: String) : String?
}