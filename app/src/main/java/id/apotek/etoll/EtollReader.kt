package id.apotek.etoll

import android.nfc.Tag
import android.nfc.tech.IsoDep
import java.io.IOException

data class EtollCard(
    val uid: String,
    val techs: List<String>,
    val type: String,
    val cardNumber: String? = null,
    val balance: Long? = null,
    val log: List<String> = emptyList(),
)

/**
 * Membaca kartu e-toll lewat NFC.
 *
 * UID selalu bisa dibaca. Nomor kartu & saldo hanya untuk Mandiri e-Money,
 * memakai perintah APDU hasil reverse-engineering komunitas (bukan dokumentasi
 * resmi bank) — hanya BACA, tidak pernah menulis/memotong saldo.
 */
object EtollReader {

    private val EMONEY_SELECT = hex("00A40400080000000000000001")
    private val EMONEY_CARD_INFO = hex("00B300003F")
    private val EMONEY_BALANCE = hex("00B500000A")

    fun read(tag: Tag): EtollCard {
        val uid = tag.id.toHex()
        val techs = tag.techList.map { it.substringAfterLast('.') }
        val isoDep = IsoDep.get(tag)
            ?: return EtollCard(uid, techs, "Tidak dikenal (bukan ISO-DEP)")

        val log = mutableListOf<String>()
        return try {
            isoDep.connect()
            isoDep.timeout = 3000
            readEmoney(isoDep, uid, techs, log)
                ?: EtollCard(uid, techs, "Tidak dikenal (Flazz/Brizzi/TapCash/lainnya?)", log = log)
        } catch (e: IOException) {
            EtollCard(uid, techs, "Gagal membaca: ${e.message} — tahan kartu lebih lama", log = log)
        } finally {
            runCatching { isoDep.close() }
        }
    }

    private fun readEmoney(
        isoDep: IsoDep,
        uid: String,
        techs: List<String>,
        log: MutableList<String>,
    ): EtollCard? {
        if (!transceive(isoDep, EMONEY_SELECT, log).isOk()) return null

        val info = transceive(isoDep, EMONEY_CARD_INFO, log)
        val cardNumber = if (info.isOk() && info.size >= 10) info.copyOf(8).toHex() else null

        val bal = transceive(isoDep, EMONEY_BALANCE, log)
        val balance = if (bal.isOk() && bal.size >= 6) bal.littleEndianInt(0) else null

        return EtollCard(uid, techs, "Mandiri e-Money", cardNumber, balance, log)
    }

    private fun transceive(isoDep: IsoDep, apdu: ByteArray, log: MutableList<String>): ByteArray {
        val resp = isoDep.transceive(apdu)
        log += "> ${apdu.toHex()}"
        log += "< ${resp.toHex()}"
        return resp
    }

    private fun ByteArray.isOk() =
        size >= 2 && this[size - 2] == 0x90.toByte() && this[size - 1] == 0x00.toByte()

    private fun ByteArray.littleEndianInt(offset: Int): Long =
        (0 until 4).fold(0L) { acc, i -> acc or ((this[offset + i].toLong() and 0xFF) shl (8 * i)) }

    private fun ByteArray.toHex() = joinToString("") { "%02X".format(it) }

    private fun hex(s: String) = ByteArray(s.length / 2) { s.substring(it * 2, it * 2 + 2).toInt(16).toByte() }
}
