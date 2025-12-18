package org.icpclive.converter.export.clics

import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.utils.io.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.html.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.elementNames
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToStream
import kotlinx.serialization.modules.SerializersModule
import org.icpclive.cds.api.AccountInfo
import org.icpclive.cds.scoreboard.ContestStateWithScoreboard
import org.icpclive.cds.util.onIdle
import org.icpclive.clics.FeedVersion
import org.icpclive.clics.clicsEventsSerializersModule
import org.icpclive.clics.events.*
import org.icpclive.clics.objects.*
import org.icpclive.converter.export.Exporter
import org.icpclive.converter.export.Router
import org.icpclive.converter.isAdminAccount
import java.io.OutputStream
import java.nio.ByteBuffer
import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.*
import kotlin.io.path.exists
import kotlin.io.path.relativeTo
import kotlin.time.Duration.Companion.minutes


internal class ClicsExporter(private val mediaDirectory: Path) : Exporter {

    @Serializable
    data class Error(val code: Int, val message: String)

    private fun ApplicationCall.hasAccess(x: ObjectWithId): Boolean {
        val whoAmI = principal<AccountInfo>()
        if (whoAmI.isAdminAccount()) return true
        return when (x) {
            is Account -> {
                whoAmI?.id?.value == x.id
            }

            else -> true
        }
    }

    private inline fun <reified T : ObjectWithId> Route.getId(
        prefix: String,
        crossinline flow: ApplicationCall.() -> Flow<Map<String, T>>,
        endpoint: MutableMap<String, SerialDescriptor>,
        module: SerializersModule,
    ) {
        endpoint[prefix] = module.getContextual(T::class)?.descriptor ?: return
        route("/$prefix") {
            get {
                call.respond(call.flow().first().entries.sortedBy { it.key }.map { it.value }.filter { call.hasAccess(it) })
            }
            get("/{id}") {
                val id = call.parameters["id"]
                val element = call.flow().first()[id]?.takeIf { call.hasAccess(it) }
                if (element != null) {
                    call.respond(element)
                } else {
                    call.respond(
                        Error(404, "Object with ID '$id' not found")
                    )
                }
            }
        }
    }

    private inline fun <reified T : Any> Route.getGlobal(
        prefix: String,
        crossinline flow: ApplicationCall.() -> Flow<T>,
        endpoint: MutableMap<String, SerialDescriptor>,
        module: SerializersModule
    ) {
        endpoint[prefix] = module.getContextual(T::class)?.descriptor ?: return
        route("/$prefix") {
            get { call.respond(call.flow().first()) }
        }
    }

    private inline fun ZipOutputStream.file(name: String, content: () -> Unit) {
        putNextEntry(ZipEntry(name))
        content()
        closeEntry()
    }

    context(json: Json)
    private inline fun <reified T> ZipOutputStream.writeJson(value: T) {
        json.encodeToStream(value, this)
    }

    context(json: Json)
    private inline fun <reified T> ZipOutputStream.jsonFile(name: String, value: T) {
        file(name) { writeJson(value) }
    }

    suspend fun formatArchive(stream: OutputStream, feed: ClicsFeedGenerator) {
        val localFiles = mutableMapOf<Path, String>()
        val with = Json {
            serializersModule = clicsEventsSerializersModule(
                FeedVersion.`2023_06`,
                "",
                urlSerializerHook = { url ->
                    if (url.value.startsWith("/media/")) {
                        val local = mediaDirectory.resolve(url.value.removePrefix("/media/"))
                            .normalize()
                            .takeIf { it.startsWith(mediaDirectory) }
                            ?.takeIf { it.exists() }
                        local?.relativeTo(mediaDirectory)
                            ?.joinToString("/", prefix = "media/")
                            ?.also { localFiles[local] = it }
                    }
                    url.value
                }
            )
            encodeDefaults = true
            explicitNulls = false
        }
        context(with) {
            ZipOutputStream(stream).use { zos ->
                with(zos) {
                    jsonFile("api.json", apiInformation(FeedVersion.`2023_06`))
                    jsonFile("contest.json", feed.contestFlow.last())
                    jsonFile("state.json",feed.stateFlow.last())
                    jsonFile("judgement-types.json", feed.judgementTypesFlow.last())
                    jsonFile("languages.json",  feed.languagesFlow.last())
                    jsonFile("problems.json",  feed.problemsFlow.last())
                    jsonFile("groups.json",  feed.groupsFlow.last())
                    jsonFile("organizations.json",  feed.organizationsFlow.last())
                    jsonFile("teams.json",  feed.teamsFlow.last())
                    jsonFile("submissions.json",  feed.submissionsFlow.last())
                    jsonFile("judgements.json",  feed.judgementsFlow.last())
                    jsonFile("commentary.json",  feed.commentaryFlow.last())
                    jsonFile("persons.json",  feed.personsFlow.last())
                    jsonFile("accounts.json",  feed.accountsFlow.last())
                    jsonFile("awards.json",  feed.awardsFlow.last())
                    jsonFile("scoreboard.json", feed.getScoreboard())
                    file("event-feed.ndjson") {
                        feed.eventFeed.collect {
                            writeJson(it.event)
                            write('\n'.code)
                        }
                    }
                    for ((local, name) in localFiles) {
                        file(name) { Files.copy(local, this) }
                    }
                }
            }
        }
    }

    override fun CoroutineScope.runOn(
        contestUpdates: Flow<ContestStateWithScoreboard>,
        adminContestUpdates: Flow<ContestStateWithScoreboard>,
    ): Router {
        val userClics = ClicsFeedGenerator(this, contestUpdates, isAdmin = false)
        val adminClics = ClicsFeedGenerator(this, adminContestUpdates, isAdmin = true)
        fun ApplicationCall.feed() = if (principal<AccountInfo>().isAdminAccount()) adminClics else userClics

        return object : Router {
            override fun HtmlBlockTag.mainPage() {
                br
                script {
                    unsafe {
                        +$$"""
                            function setClicsVersion(v) {
                                console.log("Setting clics version to " + v);
                                const prefix =`/clics/${v}api`
                                document.getElementById('clics1').href = prefix;
                                document.getElementById('clics2').href = `${prefix}/contests/contest`;
                                document.getElementById('clics3').href = `${prefix}/contests/contest/event-feed`;
                            }
                        """.trimIndent()
                    }
                }
                +"Clics feed Version:  "
                select {
                    onChange = "setClicsVersion( this.value )"
                    for (i in FeedVersion.entries) {
                        option {
                            value = if (i == FeedVersion.DRAFT) "" else "$i/"
                            if (i == FeedVersion.DRAFT) {
                                selected = true
                            }
                            +i.toString()
                        }
                    }
                }
                br
                a("/clics/api") {
                    id = "clics1"
                    +"Clics api root"
                }
                br
                a("/clics/api/contests/contest") {
                    id = "clics2"
                    +"Clics contest api root"
                }
                br
                a("/clics/api/contests/contest/event-feed") {
                    id = "clics3"
                    +"Clics event feed"
                }
                br
                a("/clics/archive.zip") {
                    id = "clics-archive"
                    +"CLICS contest archive (zip)"
                }
                br
            }

            override fun Route.setUpRoutes() {
                route("/clics/api") {
                    setupClics(FeedVersion.DRAFT)
                }
                for (version in FeedVersion.entries) {
                    if (version != FeedVersion.DRAFT) {
                        route("/clics/${version}/api") {
                            setupClics(version)
                        }
                    }
                }
                // Simple archive endpoint (placeholder empty zip for now)
                get("/clics/archive.zip") {
                    if (call.feed().stateFlow.first().finalized == null) {
                        call.respondText("Contest is not finalized yet")
                    } else {
                        call.respondOutputStream(contentType = ContentType.Application.Zip) { formatArchive(this, call.feed()) }
                    }
                }
            }

            private fun Route.setupClics(version: FeedVersion) {
                val clicsEventsSerializersModule = clicsEventsSerializersModule(version, tokenPrefix = "")
                val endpoint = mutableMapOf<String, SerialDescriptor>()
                val json = Json {
                    encodeDefaults = true
                    prettyPrint = false
                    explicitNulls = false
                    serializersModule = clicsEventsSerializersModule
                }
                install(ContentNegotiation) { json(json) }
                get {
                    if (clicsEventsSerializersModule.getContextual(ApiInformation::class) != null) {
                        call.respond(
                            apiInformation(version)
                        )
                    }
                }
                route("/contests") {
                    get { call.respond(listOf(call.feed().contestFlow.first())) }
                    getGlobal("contest", { feed().contestFlow }, endpoint, clicsEventsSerializersModule)
                    route("contest") {
                        getGlobal("state", { feed().stateFlow }, endpoint, clicsEventsSerializersModule)
                        getId("judgement-types", { feed().judgementTypesFlow }, endpoint, clicsEventsSerializersModule)
                        getId("languages", { feed().languagesFlow }, endpoint, clicsEventsSerializersModule)
                        getId("problems", { feed().problemsFlow }, endpoint, clicsEventsSerializersModule)
                        getId("groups", { feed().groupsFlow }, endpoint, clicsEventsSerializersModule)
                        getId("organizations", { feed().organizationsFlow }, endpoint, clicsEventsSerializersModule)
                        getId("teams", { feed().teamsFlow }, endpoint, clicsEventsSerializersModule)
                        getId("submissions", { feed().submissionsFlow }, endpoint, clicsEventsSerializersModule)
                        getId("judgements", { feed().judgementsFlow }, endpoint, clicsEventsSerializersModule)
                        //getId("runs", runsFlow, endpoint, clicsEventsSerializersModule)
                        getId("commentary", { feed().commentaryFlow }, endpoint, clicsEventsSerializersModule)
                        getId("persons", { feed().personsFlow }, endpoint, clicsEventsSerializersModule)
                        getId("accounts", { feed().accountsFlow }, endpoint, clicsEventsSerializersModule)
                        //getId("clarifications", clarificationsFlow, endpoint, clicsEventsSerializersModule)
                        getId("awards", { feed().awardsFlow }, endpoint, clicsEventsSerializersModule)
                        get("/scoreboard") { call.respond(call.feed().getScoreboard()) }
                        get("/event-feed") {
                            call.respondBytesWriter(contentType = ContentType("application", "x-ndjson")) {
                                call.feed().eventFeed
                                    .filter { clicsEventsSerializersModule.getContextual(it.event::class) != null }
                                    .filter {
                                        when (it.event) {
                                            is IdEvent<*> -> call.hasAccess(it.event.data ?: (it.oldValue as ObjectWithId))
                                            is BatchEvent<*>, is PreloadFinishedEvent -> error("Not expected here")
                                            is GlobalEvent<*> -> true
                                        }
                                    }
                                    .map { json.encodeToString(it.event) }
                                    .onIdle(1.minutes) { channel.send("") }
                                    .collect {
                                        writeFully(ByteBuffer.wrap("$it\n".toByteArray()))
                                        flush()
                                    }
                            }
                        }
                        get("/access") {
                            call.respond(
                                Access(
                                    emptyList(),
                                    endpoint.entries.map { (k, v) ->
                                        Endpoint(
                                            k,
                                            v.elementNames.toList()
                                        )
                                    }
                                )
                            )
                        }
                    }
                }
            }

        }
    }

    private fun apiInformation(version: FeedVersion): ApiInformation = ApiInformation(
        version = version.name,
        versionUrl = version.url,
        provider = ApiInformationProvider(
            name = "icpc live"
        )
    )

}
