package id.apotek.etoll

import android.app.Activity
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Bundle
import android.widget.TextView
import java.text.NumberFormat
import java.util.Locale

class MainActivity : Activity(), NfcAdapter.ReaderCallback {

    private var nfc: NfcAdapter? = null
    private lateinit var status: TextView
    private lateinit var result: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        status = findViewById(R.id.status)
        result = findViewById(R.id.result)
        nfc = NfcAdapter.getDefaultAdapter(this)
    }

    override fun onResume() {
        super.onResume()
        val adapter = nfc
        when {
            adapter == null -> status.setText(R.string.nfc_missing)
            !adapter.isEnabled -> status.setText(R.string.nfc_disabled)
            else -> {
                status.setText(R.string.tap_card)
                adapter.enableReaderMode(
                    this,
                    this,
                    NfcAdapter.FLAG_READER_NFC_A or
                        NfcAdapter.FLAG_READER_NFC_B or
                        NfcAdapter.FLAG_READER_NFC_F or
                        NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK,
                    null,
                )
            }
        }
    }

    override fun onPause() {
        super.onPause()
        nfc?.disableReaderMode(this)
    }

    // Dipanggil di thread NFC, bukan UI thread.
    override fun onTagDiscovered(tag: Tag) {
        val card = EtollReader.read(tag)
        runOnUiThread { show(card) }
    }

    private fun show(card: EtollCard) {
        val rupiah = NumberFormat.getCurrencyInstance(Locale("in", "ID"))
        status.text = card.type
        result.text = buildString {
            appendLine("UID       : ${card.uid}")
            appendLine("Teknologi : ${card.techs.joinToString()}")
            card.cardNumber?.let { appendLine("No. kartu : ${it.chunked(4).joinToString(" ")}") }
            card.balance?.let { appendLine("Saldo     : ${rupiah.format(it)}") }
            if (card.log.isNotEmpty()) {
                appendLine()
                appendLine("Log APDU:")
                card.log.forEach { appendLine(it) }
            }
        }
    }
}
