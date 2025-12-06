package org.icpclive.cds.api

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.serializer
import org.icpclive.cds.settings.Credential
import org.icpclive.cds.util.DelegatedSerializer

@JvmInline
@Serializable
public value class AccountId internal constructor(public val value: String) {
    override fun toString(): String = value
}

public fun String.toAccountId(): AccountId = AccountId(this)
public fun Int.toAccountId(): AccountId = toString().toAccountId()
public fun Long.toAccountId(): AccountId = toString().toAccountId()


@Serializable
public data class AccountInfo(
    val id: AccountId,
    @EncodeDefault(EncodeDefault.Mode.ALWAYS) val username: String = id.value,
    val type: String,
    @Serializable(with = AccountCredentialSerializer::class) val password: Credential? = null,
    val name: String? = null,
    val teamId: TeamId? = null,
    val personId: PersonId? = null,
)

internal class AccountCredentialSerializer: DelegatedSerializer<Credential, String>(String.serializer()) {
    override fun onDeserialize(value: String) = Credential("***", value)
    override fun onSerialize(value: Credential) = value.displayValue
}