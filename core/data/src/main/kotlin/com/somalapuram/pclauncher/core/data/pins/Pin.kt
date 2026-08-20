package com.somalapuram.pclauncher.core.data.pins

/**
 * One pinned app, stored flat.
 *
 * A component and a user serial rather than an `AppKey`: `core:data` must not depend on
 * `core:apps`, and the store has to be readable without a `UserHandle`, which cannot be
 * constructed from persisted data anyway.
 */
data class Pin(
    /** `ComponentName.flattenToShortString()`. */
    val component: String,
    val userSerial: Long,
)

/**
 * The pins, in the order the user arranged them.
 *
 * Order is data, not a sort: it is the whole point of a dock. Stored as a sequence rather than a
 * map with an index field, because the two would eventually disagree.
 */
data class Pins(val items: List<Pin> = emptyList()) {

    fun contains(pin: Pin): Boolean = items.contains(pin)

    /**
     * Idempotent: pinning something already pinned changes nothing, **including not moving it to
     * the end**. A user who pins twice by accident should not find their dock rearranged.
     */
    fun plus(pin: Pin): Pins = if (contains(pin)) this else Pins(items + pin)

    /** Removing something absent is a no-op, not an error. */
    fun minus(pin: Pin): Pins = if (contains(pin)) Pins(items - pin) else this

    val size: Int get() = items.size
    val isEmpty: Boolean get() = items.isEmpty()
}

/** Encodes [Pins] for storage. Kept pure and separate so round-tripping is directly testable. */
object PinCodec {

    private const val ENTRY_SEPARATOR = "\n"
    private const val FIELD_SEPARATOR = "|"

    fun encode(pins: Pins): String = pins.items.joinToString(ENTRY_SEPARATOR) {
        "${it.component}$FIELD_SEPARATOR${it.userSerial}"
    }

    /**
     * Decodes, skipping anything malformed.
     *
     * A corrupted line loses one pin; throwing here would lose the whole dock, and this store is
     * read on the way to drawing the home screen (GATE 4).
     */
    fun decode(raw: String?): Pins {
        if (raw.isNullOrBlank()) return Pins()
        val items = raw.split(ENTRY_SEPARATOR).mapNotNull { line ->
            val parts = line.split(FIELD_SEPARATOR)
            if (parts.size != 2) return@mapNotNull null
            val serial = parts[1].toLongOrNull() ?: return@mapNotNull null
            val component = parts[0].takeIf { it.isNotBlank() } ?: return@mapNotNull null
            Pin(component, serial)
        }
        return Pins(items.distinct())
    }
}
