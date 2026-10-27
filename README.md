# Apotek — Baca Kartu E-Toll (Android)

Aplikasi Android sederhana untuk membaca kartu e-toll via NFC.

| Data | Kartu |
|---|---|
| UID chip | Semua kartu (e-Money, Flazz, Brizzi, TapCash, JakCard) |
| Nomor kartu & saldo | Mandiri e-Money saja (APDU hasil reverse-engineering komunitas, belum resmi) |

Hanya **membaca**. Potong saldo / top-up butuh SAM dari bank (kerja sama merchant resmi).

## Menjalankan
1. Buka folder ini di Android Studio.
2. Sambungkan HP Android yang punya NFC (NFC dalam keadaan aktif).
3. Run, lalu tempelkan kartu ke bagian belakang HP.

Kode utama: `app/src/main/java/id/apotek/etoll/EtollReader.kt`.
