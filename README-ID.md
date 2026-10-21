# WARUNG KASIR — Android Offline

Aplikasi Android native untuk HP dan tablet, dengan database SQLite lokal.

## Fitur
- Kasir: keranjang, total, pembayaran dan kembalian.
- Data barang: tambah, edit, hapus, harga jual/modal, stok.
- Stok berkurang otomatis setelah transaksi berhasil.
- Riwayat transaksi dan ringkasan omzet/laba harian.
- Tampilan struk di layar.
- Cetak ke printer Bluetooth yang sudah dipasangkan dan mendukung ESC/POS melalui koneksi serial Bluetooth (SPP).
- Tidak memerlukan internet saat digunakan. Internet hanya diperlukan untuk proses build melalui GitHub Actions.

## Cara mendapatkan APK tanpa Android Studio
1. Buat akun GitHub atau masuk ke akun GitHub.
2. Buat repository baru bernama `WarungKasir`, pilih **Public** atau **Private**.
3. Unggah semua isi folder ZIP ini ke repository (jangan unggah ZIP-nya saja; ekstrak dulu).
4. Buka tab **Actions**, pilih **Build Warung Kasir APK**, lalu tekan **Run workflow**. Jika tab Actions meminta izin, aktifkan workflow.
5. Tunggu proses selesai dengan tanda hijau. Buka hasil run tersebut, cari bagian **Artifacts**, lalu unduh `WarungKasir-debug-apk`.
6. Ekstrak ZIP artifact yang diunduh. Di dalamnya ada `app-debug.apk`.

## Cara instal di HP Android
1. Kirim `app-debug.apk` ke HP (USB, Bluetooth, atau penyimpanan awan).
2. Buka file APK di HP.
3. Jika diminta, izinkan **Install unknown apps / Pasang aplikasi tidak dikenal** untuk aplikasi File Manager atau browser yang dipakai.
4. Tekan **Install**, lalu buka **Warung Kasir**.
5. Aplikasi berisi contoh barang awal. Edit/hapus contoh tersebut dan masukkan barang warung sendiri.

## Cara cetak struk
1. Nyalakan printer thermal Bluetooth.
2. Buka Pengaturan Android > Bluetooth, pasangkan printer terlebih dahulu.
3. Selesaikan transaksi; pada dialog struk pilih **Cetak Bluetooth** lalu pilih nama printer.
4. Printer harus mendukung ESC/POS melalui Bluetooth Serial Port Profile (SPP). Printer BLE-only atau printer dengan protokol khusus mungkin tidak kompatibel.

## Catatan penting
- APK debug dibuat untuk uji coba, bukan rilis Play Store.
- Data tersimpan pada database aplikasi di perangkat. Menghapus aplikasi dapat menghapus data; lakukan ekspor/cadangan sebelum menghapus aplikasi.
- Aplikasi ini belum memiliki fitur ekspor/cadangan database, barcode scanner kamera, retur, diskon, atau multi-kasir.
- Laporan harian menghitung transaksi berdasarkan tanggal lokal perangkat.
- Cetak Bluetooth tergantung model printer dan encoding karakter yang didukung; uji struk sebelum dipakai untuk transaksi penting.
- Proyek memerlukan koneksi internet pada mesin build untuk mengunduh Gradle dan plugin Android.
