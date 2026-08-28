# Panduan Instalasi Rhpatch

Rhpatch adalah modul Xposed tingkat lanjut yang dirancang untuk memperluas fungsionalitas aplikasi dengan mulus. Karena bergantung pada kerangka kerja Xposed, perangkat Anda harus di-root dan memiliki kerangka kerja yang kompatibel (seperti LSPosed) terpasang.

## Prasyarat
1. Perangkat Android yang sudah di-root (Magisk atau KernelSU sangat disarankan).
2. Framework LSPosed atau EdXposed terpasang dan aktif.
3. Aplikasi target (seperti WhatsApp, Telegram, dll) dalam versi yang didukung.

## Langkah-Langkah Instalasi
1. Unduh rilis APK terbaru dari halaman [Releases GitHub](https://github.com/Rhdevs71/apahayo/releases).
2. Instal APK `Rhpatch` seperti biasa pada perangkat Anda.
3. Buka manajer LSPosed. Modul `Rhpatch` akan muncul di tab "Modules".
4. Aktifkan modul `Rhpatch`.
5. Centang aplikasi sistem yang disarankan dan aplikasi target (misal: WhatsApp, Telegram).
6. **Reboot (Mulai Ulang)** perangkat Anda agar modul mulai bekerja.
7. Buka aplikasi Rhpatch dari *app drawer* atau *shortcut*, lalu periksa halaman Dashboard untuk memastikan status "Active" (berwarna hijau).

## Pemecahan Masalah
- Jika status masih "Inactive" setelah reboot, pastikan Anda telah mencentang aplikasi target di LSPosed Manager.
- Periksa menu "Diagnostics" di Dashboard Rhpatch untuk melihat *system log* dan menemukan titik masalah.
