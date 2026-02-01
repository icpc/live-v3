package org.icpclive.cds.api

import kotlinx.serialization.*
import org.icpclive.cds.settings.Credential
import org.icpclive.cds.util.map

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

public object AccountCredentialSerializer: KSerializer<Credential> by serializer<String>().map(
    "AccountCred",
    { Credential("***", it) },
    { it.displayValue })