package org.icpclive.cds.util.serializers

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import org.icpclive.cds.util.getLogger
import java.awt.Color

public object ColorSerializer : KSerializer<Color> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Color", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Color) {
        encoder.encodeString("#%02x%02x%02x%02x".format(value.red, value.green, value.blue, value.alpha))
    }

    override fun deserialize(decoder: Decoder): Color {
        val data = decoder.decodeString()
        return try {
            if (data.startsWith("0x")) {
                return Color(data.toUInt(radix = 16).toInt(), data.length == 8)
            }
            val str = data.removePrefix("#")
            when (str.length) {
                8 -> Color(
                    str.substring(0, 2).toInt(radix = 16),
                    str.substring(2, 4).toInt(radix = 16),
                    str.substring(4, 6).toInt(radix = 16),
                    str.substring(6, 8).toInt(radix = 16),
                )

                6 -> Color(
                    str.substring(0, 2).toInt(radix = 16),
                    str.substring(2, 4).toInt(radix = 16),
                    str.substring(4, 6).toInt(radix = 16),
                )

                3 -> Color(
                    str[0].digitToInt(16) * 0x11,
                    str[1].digitToInt(16) * 0x11,
                    str[2].digitToInt(16) * 0x11,
                )

                else -> {
                    throw NumberFormatException()
                }
            }
        } catch (e: NumberFormatException) {
            log.error(e) { "Failed to parse color from $data" }
            Color.BLACK
        }
    }

    private val log by getLogger()
}