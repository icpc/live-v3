@file:Suppress("unused")

package org.icpclive.converter

import com.github.ajalt.clikt.core.*
import com.github.ajalt.clikt.output.MordantHelpFormatter
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.multiple
import com.github.ajalt.mordant.terminal.danger
import com.github.ajalt.mordant.terminal.info
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.html.*
import io.ktor.server.http.content.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.html.*
import org.icpclive.cds.InfoUpdate
import org.icpclive.cds.adapters.addComputedData
import org.icpclive.cds.api.*
import org.icpclive.cds.scoreboard.ContestStateWithScoreboard
import org.icpclive.cds.scoreboard.calculateScoreboard
import org.icpclive.cds.util.*
import org.icpclive.converter.commands.*
import org.icpclive.converter.export.Exporter
import org.icpclive.converter.export.Router
import org.icpclive.converter.export.clics.ClicsExporter
import org.icpclive.converter.export.icpc.IcpcCsvExporter
import org.icpclive.converter.export.pcms.PCMSHtmlExporter
import org.icpclive.converter.export.pcms.PCMSXmlExporter
import org.icpclive.converter.export.reactions.ReactionsExporter
import org.icpclive.server.*
import kotlin.system.exitProcess


object MainCommand : CliktCommand(name = "java -jar cds-converter.jar") {
    init {
        context {
            helpFormatter = { MordantHelpFormatter(it, showRequiredTag = true, showDefaultValues = true)}
        }
    }
    override val invokeWithoutSubcommand = true
    override val treatUnknownOptionsAsArgs = true
    val unused by argument().multiple()
    override fun run() {
        if (currentContext.invokedSubcommand == null) {
            if (unused.isNotEmpty()) {
                currentContext.terminal.danger("Unknown command ${unused.firstOrNull()}")
                currentContext.terminal.info("")
            }
            throw PrintHelpMessage(currentContext, true)
        }
    }
}

fun main(args: Array<String>): Unit = MainCommand.subcommands(
    PCMSDumpCommand,
    PCMSScoreboardDumpCommand,
    ClicsArchiveCommand,
    ServerCommand,
    IcpcCSVDumpCommand
).main(args)


private val logger by getLogger()


@Suppress("unused") // application.yaml references the main function. This annotation prevents the IDE from marking it as unused.
fun Application.module() {
    setupDefaultKtorPlugins()

    val handler = CoroutineExceptionHandler { coroutineContext, throwable ->
        environment.log.error("Uncaught exception in coroutine context $coroutineContext", throwable)
        exitProcess(1)
    }

    ServerCommand.publisher?.let { startPublisher(it) }

    val routers = mutableListOf<Router>()

    val scope = this + handler

    val accounts: StateFlow<Map<String, AccountInfo>>

    fun Exporter.run(
        nonAdminScope: SharedFlowSubscriptionScope<ContestStateWithScoreboard>,
        adminScope: SharedFlowSubscriptionScope<ContestStateWithScoreboard>,
    )  {
        nonAdminScope.withSubscription(subscriptionCount) { nonAdminFlow ->
            adminScope.withSubscription(subscriptionCount) { adminFlow ->
                routers += (scope + CoroutineName(this@run::class.simpleName!!)).runOn(
                    contestUpdates = nonAdminFlow.onStart { logger.info { "Exporter ${this@run::class.simpleName} subscribed to user cds data" } },
                    adminContestUpdates = adminFlow.onStart { logger.info { "Exporter ${this@run::class.simpleName} subscribed to admin cds data" } },
                )
            }
        }
    }

    ServerCommand.cdsOptions.toFlow().shareWith(scope) {
        withSubscription(3) { rootFlow ->
            accounts = rootFlow.filterIsInstance<InfoUpdate>().map {
                it.newInfo.accounts.values.associateBy { it.username }
            }.stateIn(scope, SharingStarted.Eagerly, emptyMap())
            val nonAdminFlow = rootFlow.addComputedData {
                submissionResultsAfterFreeze = false
                submissionsAfterEnd = false
                autoFinalize = !ServerCommand.cdsOptions.noAutoFinalize
            }.calculateScoreboard(OptimismLevel.NORMAL)
            val adminFlow = rootFlow.addComputedData {
                submissionResultsAfterFreeze = true
                submissionsAfterEnd = ServerCommand.cdsOptions.upsolving
                autoFinalize = !ServerCommand.cdsOptions.noAutoFinalize
            }.calculateScoreboard(OptimismLevel.NORMAL)
            nonAdminFlow.shareWith(scope) nonAdmin@{
                adminFlow.shareWith(scope) admin@{
                    PCMSXmlExporter.run(this@nonAdmin, this@admin)
                    PCMSHtmlExporter.run(this@nonAdmin, this@admin)
                    IcpcCsvExporter.run(this@nonAdmin, this@admin)
                    ClicsExporter(ServerCommand.mediaDirectory).run(this@nonAdmin, this@admin)
                    ReactionsExporter.run(this@nonAdmin, this@admin)
                }
            }
        }
    }

    routing {
        install(ContentNegotiation) { json(serverResponseJsonSettings()) }
        install(Authentication) {
            basic("auth") {
                realm = "Access to cds-converter"
                validate {
                    val account = accounts.value[it.name] ?: return@validate null
                    if (it.password == account.password?.value) {
                        account
                    } else {
                        null
                    }
                }
            }
            val unknownUser = AccountInfo("unknown-user".toAccountId(), "unknown", type = "other")
            val unknownAdmin = AccountInfo("unknown-admin".toAccountId(), "unknown", type = "admin")
            val guestConfig = object: AuthenticationProvider.Config("guest") {}
            register(object : AuthenticationProvider(guestConfig) {
                override suspend fun onAuthenticate(context: AuthenticationContext) {
                    if (accounts.value.none { it.value.isAdminAccount() }) {
                        context.principal(guestConfig.name, unknownAdmin)
                    } else {
                        context.principal(guestConfig.name, unknownUser)
                    }
                }
            })
        }
        staticFiles("/media", ServerCommand.mediaDirectory.toFile())

        authenticate("auth", optional = false) {
            get("/login") {
                call.respondRedirect("/")
            }
        }
        get("/logout") {
            call.respondHtml(HttpStatusCode.Unauthorized) {
                body {
                    meta { httpEquiv = "refresh"; content = "0; url=/" }
                }
            }
        }
        authenticate("auth", optional = true) {
            get {
                call.respondHtml {
                    body {
                        for (router in routers) {
                            with (router) {
                                mainPage()
                                br
                            }
                        }
                        val user = call.principal<AccountInfo>()
                        if (user == null) {
                            a("/login") { +"Login" }
                            br
                        } else {
                            +"Logged in as ${user.username} (isAdmin = ${user.isAdminAccount()})"
                            br
                            a("/logout") { +"Logout" }
                            br
                        }
                    }
                }
            }

        }
        authenticate("auth", "guest", strategy = AuthenticationStrategy.FirstSuccessful) {
            for (router in routers) {
                with(router) {
                    setUpRoutes()
                }
            }
        }
    }

    log.info("Configuration is done")
}

fun AccountInfo?.isAdminAccount() = this?.type == "admin"