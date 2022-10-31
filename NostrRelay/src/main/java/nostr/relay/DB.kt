package nostr.relay

import nostr.relay.Events.pubKey
import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable

object Events : IntIdTable() {
    val hash = char("hash", 64).uniqueIndex()
    val pubKey = char("pubKey", 64)
    val kind = integer("kind")
    val raw = text("raw")
    val createdAt = long("createdAt")
    val hidden = bool("hidden").default(false)
    val firstSeen = long("firstSeen").default(System.currentTimeMillis())
    val dTag = text("dTag").nullable().index()
}

class DbEvent(id: EntityID<Int>) : Entity<Int>(id) {
    companion object : EntityClass<Int, DbEvent>(Events)

    var hash by Events.hash
    var publicKey by pubKey
    var kind by Events.kind
    var raw by Events.raw
    var createdAt by Events.createdAt
    var hidden by Events.hidden
    var firstSeen by Events.firstSeen
    var dTag by Events.dTag
}

object Tags : IntIdTable() {
    val event = integer("event_id").references(Events.id)
    val key = varchar("key", length = 20).index()
    val value = text("value").index()
}

class DbTag(id: EntityID<Int>) : Entity<Int>(id) {
    companion object : EntityClass<Int, DbTag>(Tags)

    var event by Tags.event
    var key by Tags.key
    var value by Tags.value
}
