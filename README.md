<div align="center">
  <img src="https://raw.githubusercontent.com/Rhdevs71/apahayo/main/app/src/main/res/drawable/ic_launcher_new.jpg" width="128" height="128" style="border-radius: 28px;" alt="Rhpatch Logo">
  <h1>⚡ Rhpatch (Apahayo)</h1>
  <p><strong>The All-in-One Multi-Target Xposed Customization & Modification Suite for Android</strong></p>

  <p>
    <a href="https://github.com/Rhdevs71/apahayo/releases"><img src="https://img.shields.io/github/v/release/Rhdevs71/apahayo?color=7C3AED&style=for-the-badge&logo=github" alt="Latest Release"></a>
    <a href="https://t.me/+eJNVZK7qKE41Yjdl"><img src="https://img.shields.io/badge/Telegram-Community-0088cc?style=for-the-badge&logo=telegram" alt="Telegram Channel"></a>
    <img src="https://img.shields.io/badge/Android-9.0%2B%20(API%2028%2B)-3DDC84?style=for-the-badge&logo=android" alt="Android Version">
    <img src="https://img.shields.io/badge/Xposed%20API-93%20(LSPosed)-orange?style=for-the-badge" alt="Xposed API">
    <a href="LICENSE"><img src="https://img.shields.io/github/license/Rhdevs71/apahayo?color=10B981&style=for-the-badge" alt="License"></a>
  </p>

  <p>
    <a href="#-english"><strong>English</strong></a> • <a href="#-bahasa-indonesia"><strong>Bahasa Indonesia</strong></a>
  </p>
</div>

---

<a name="-english"></a>
# 🇬🇧 English Documentation

## 📖 Overview
**Rhpatch (Apahayo)** is an advanced, modular Android Xposed modification framework. Built with performance, privacy, and user empowerment in mind, Rhpatch hooks runtime applications at the memory level (0% lag, native 60/120 FPS performance) to unlock premium features, eliminate advertisements, filter clutter, and provide granular privacy controls across popular Android applications.

---

## ✨ Features by Module

### 🟢 1. WhatsApp Module
* **Anti-Revoke / Anti-Delete**: Never miss deleted messages; read unsend messages and deleted status updates.
* **Stealth & Privacy**: Ghost mode, hide blue ticks, hide second tick, freeze last seen, and hide typing/recording indicators.
* **Media & Status Downloader**: Save HD stories, profile pictures, and view-once media directly to your gallery.
* **Automated Message Scheduler**: Schedule outgoing text, media, and documents at specific dates/times.
* **Smart Auto-Reply**: Rule-based automatic responder triggered by incoming notifications.
* **Custom Chat Folders & Organizers**: Categorize conversations into custom tabs and folders.
* **Simulated Incoming Call**: Trigger realistic scheduled fake calls for security or meeting exits.
* **In-App Audio Player**: Advanced playback speed control and waveform seeking for voice notes.

### 🎵 2. TikTok Module
* **Top Feed Tab Filter**: Toggle visibility for individual top tabs — **STEM**, **Shop/Toko**, **Explore/Jelajah**, **LIVE**, **Community**, and **Location/Nearby (Jakarta, Bekasi, etc.)**.
* **Clean Feed Content Filter (Java Model Hook)**:
  * Automatically removes sponsored Ads & promoted videos.
  * Filters out TikTok Shop banners & product overlays.
  * Hides Live broadcasts & Live replay cards.
  * Removes Story posts and Photo Slides / Image posts (video-only mode).
  * Filter videos based on custom minimum/maximum view and like thresholds.
* **Tako AI Controller Suppression**: Automatically disables and collapses the Tako AI chat bubble without UI stutter.
* **Regional Restriction Bypass**: Custom SIM operator spoofing (MCC/MNC, ISO country codes) to unlock geo-locked content globally.
* **Player Improvements**: Force seekbar timeline display, video thumbnail preview seeking, and loop toggles.

### 📸 3. Instagram Module
* **Native Media Downloader**: Download high-resolution Reels, Feed videos, and Carousel photos with a single tap.
* **Ghost Story & DM**: View Stories and read Direct Messages without sending read receipts or triggers.
* **Ad & Suggestion Removal**: Strips sponsored ads, suggested posts, and recommended user grids from your feed.
* **Direct Comment Copy**: Long-press to copy any comment text directly to your clipboard.

### ▶️ 4. YouTube Module
* **SponsorBlock Integration**: Automatically skips sponsor segments, intros, outros, and subscribe reminders.
* **Return YouTube Dislike (RYD)**: Restores the public dislike counter using the official RYD API.
* **Background Playback**: Continuous background audio and Picture-in-Picture (PiP) for all videos.
* **Smart Swipe Controls**: Adjust volume and screen brightness by swiping on the video player edges.

### 🛡️ 5. Multi-App Patch Suite (20+ Apps)
Pre-built runtime optimizations and premium feature unlockers for:
* **Productivity & Utilities**: AccuBattery Pro, Adobe Scan, CamScanner, Cloudflare 1.1.1.1 WARP, MacroDroid Pro, RAR Premium, SD Maid SE, Speedtest by Ookla.
* **Social & Messaging**: Discord, Facebook, Messenger, Telegram, Twitch.
* **Learning & Lifestyle**: Duolingo Super, Getcontact Premium, Google Photos (Pixel Spoofing), IbisPaint X, Kahoot, KineMaster, Lightroom, MangaPlus, Photomath, Sticker.ly, Strava, WolframAlpha.

---

## 📲 Step-by-Step Installation

### Prerequisites
* Rooted Android Device (Magisk / KernelSU / APatch).
* Working Xposed Framework (Recommended: [**LSPosed (Zygisk)**](https://t.me/LSPosed/321)).
* Android 9.0 (API 28) up to Android 15+.

### Setup Instructions
1. Download the latest `RHpatch-*.apk` from the [Releases](https://github.com/Rhdevs71/apahayo/releases) page.
2. Install the APK on your device.
3. Open **LSPosed Manager** > Go to the **Modules** tab.
4. Enable **Rhpatch**.
5. Select your desired target application scopes (e.g. *WhatsApp, TikTok, Instagram, YouTube*).
6. **Force Stop** or **Reboot** your device to allow the hooks to initialize cleanly.
7. Open the Rhpatch app to customize your preferences.

---

## 🔒 Permission Requirements & Transparency

Rhpatch requires the following Android permissions to power its features:

| Permission | Purpose & Requirement |
| :--- | :--- |
| **`BIND_ACCESSIBILITY_SERVICE`** | Used strictly by the **Universal Auto Sender** to automate scheduled message dispatching without requiring root shell input simulation. |
| **`BIND_NOTIFICATION_LISTENER`** | Required by the **Smart Auto-Reply** engine to detect incoming messages and trigger automated responses. |
| **`MANAGE_EXTERNAL_STORAGE` / `READ_MEDIA`** | Needed to save downloaded videos, photos, status updates, and audio voice recordings directly to your gallery/storage. |
| **`REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`** | Prevents Android OEM battery killers from killing the background message scheduler alarm. |
| **`SYSTEM_ALERT_WINDOW`** | Used to display floating download progress overlays and simulated incoming call screens. |

---

<a name="-bahasa-indonesia"></a>
# 🇮🇩 Dokumentasi Bahasa Indonesia

## 📖 Ringkasan
**Rhpatch (Apahayo)** adalah modul Xposed multifungsi all-in-one yang dirancang untuk memberikan kendali penuh atas aplikasi Android favorit Anda. Bekerja langsung di level memori sistem (*0% lag, performa asli 60/120 FPS*), Rhpatch membuka fitur premium, membersihkan iklan yang mengganggu, menyaring konten tidak diinginkan, dan memberikan privasi tingkat lanjut tanpa membebani performa perangkat.

---

## ✨ Rincian Fitur Modul

### 🟢 1. Modul WhatsApp
* **Anti Hapus Pesan & Status**: Pesan dan status yang ditarik/dihapus oleh pengirim tetap dapat dibaca dan disimpan.
* **Mode Privasi & Hantu**: Sembunyikan centang biru, centang dua, status online, serta status sedang mengetik/merekam suara.
* **Pengunduh Media & Status**: Unduh status foto/video HD dan media sekali lihat (*view-once*) langsung ke galeri.
* **Penjadwal Pesan Otomatis**: Atur pesan teks dan media untuk terkirim otomatis pada tanggal dan jam tertentu.
* **Balas Pesan Otomatis (Auto-Reply)**: Respon pesan masuk otomatis berdasarkan aturan kata kunci.
* **Folder & Kategori Chat**: Susun dan kelompokkan obrolan WhatsApp ke dalam tab folder kustom.
* **Simulasi Panggilan Palsu**: Buat panggilan masuk tiruan terjadwal untuk situasi darurat atau keluar dari pertemuan.

### 🎵 2. Modul TikTok
* **Filter Tab Navigasi Atas**: Sembunyikan tab atas individual sesuai keinginan — **STEM**, **Toko (Shop)**, **Jelajah (Explore)**, **LIVE**, **Komunitas**, dan **Lokasi/Sekitar (Jakarta, Bekasi, Surabaya, dll.)**.
* **Filter Konten Feed Bersih (Java Model Hook)**:
  * Hapus iklan promosi dan video bersponsor secara otomatis.
  * Hilangkan banner belanja TikTok Shop dan keranjang kuning.
  * Sembunyikan siaran langsung (Live) dan tayangan ulang Live di beranda.
  * Sembunyikan postingan Story dan postingan slide Foto (hanya menampilkan video murni).
  * Filter video berdasarkan batas minimal/maksimal jumlah tayangan (*views*) dan *likes*.
* **Penghilang Gelembung Tako AI**: Memadamkan dan mengecilkan tombol chat Tako AI secara aman tanpa force close.
* **Bypass Batasan Wilayah & Palsukan SIM**: Buka blokir konten antar negara dengan memalsukan kode negara ISO dan MCC/MNC operator.
* **Peningkatan Pemutar**: Paksa timeline seekbar selalu muncul, aktifkan pratinjau thumbnail saat menggeser video, dan atur pemutaran ulang.

### 📸 3. Modul Instagram
* **Pengunduh Media Terintegrasi**: Unduh Reels, video Feed, dan foto Carousel beresolusi tinggi dengan sekali klik.
* **Mode Hantu Story & DM**: Lihat Story dan baca Direct Message tanpa meninggalkan jejak terbaca (*seen*).
* **Pembersih Iklan & Rekomendasi**: Bersihkan postingan bersponsor dan rekomendasi akun dari timeline beranda.
* **Salin Teks Komentar**: Tekan lama komentar apapun untuk langsung menyalin teksnya ke clipboard.

### ▶️ 4. Modul YouTube
* **SponsorBlock**: Lewati segmen sponsor, intro, outro, dan pengingat subscribe secara otomatis.
* **Kembalikan Jumlah Dislike (RYD)**: Menampilkan kembali jumlah dislike publik menggunakan API resmi Return YouTube Dislike.
* **Pemutaran Latar Belakang**: Putar audio di latar belakang dan mode Gambar-dalam-Gambar (PiP).
* **Kontrol Usap (Swipe Gestures)**: Atur volume dan kecerahan layar dengan mengusap tepi pemutar video.

### 🛡️ 5. Paket Modul Aplikasi Lainnya (20+ Aplikasi)
Optimasi runtime dan fitur premium untuk:
* **Produktivitas**: AccuBattery Pro, Adobe Scan, CamScanner, Cloudflare 1.1.1.1 WARP, MacroDroid Pro, RAR Premium, SD Maid SE, Speedtest by Ookla.
* **Sosial & Chat**: Discord, Facebook, Messenger, Telegram, Twitch.
* **Edukasi & Media**: Duolingo Super, Getcontact Premium, Google Photos (Pixel Spoofing), IbisPaint X, Kahoot, KineMaster, Lightroom, MangaPlus, Photomath, Sticker.ly, Strava, WolframAlpha.

---

## 📲 Panduan Pemasangan Langkah Demi Langkah

### Persyaratan
* Perangkat Android dalam kondisi Root (Magisk / KernelSU / APatch).
* Framework Xposed terpasang (Disarankan: [**LSPosed Versi 321**](https://t.me/LSPosed/321)).
* Android 9.0 (API 28) hingga Android 15+.

### Langkah-Langkah
1. Unduh APK terbaru `RHpatch-*.apk` dari menu [Releases](https://github.com/Rhdevs71/apahayo/releases).
2. Pasang (install) APK pada HP Anda.
3. Buka aplikasi **LSPosed Manager** > Masuk ke tab **Modul**.
4. Aktifkan modul **Rhpatch**.
5. Centang aplikasi target yang ingin dimodifikasi (misal: *WhatsApp, TikTok, Instagram, YouTube*).
6. Lakukan **Paksa Berhenti (Force Stop)** pada aplikasi target atau **Mulai Ulang (Reboot)** HP Anda.
7. Buka aplikasi Rhpatch untuk menyesuaikan pengaturan sesuai keinginan Anda.

---

## 🔒 Transparansi & Keperluan Izin Aplikasi

Rhpatch memerlukan beberapa izin sistem Android untuk menjalankan fiturnya:

| Izin Sistem | Tujuan & Fungsi Penggunaan |
| :--- | :--- |
| **`Layanan Aksesibilitas`** | Digunakan khusus oleh fitur **Pengirim Pesan Otomatis** untuk mengirim pesan terjadwal tanpa memerlukan emulasi input root. |
| **`Akses Notifikasi`** | Diperlukan oleh fitur **Balas Pesan Otomatis** untuk membaca pesan masuk dan merespons pesan secara cerdas. |
| **`Akses Penyimpanan & Media`** | Diperlukan untuk menyimpan video, foto, story, dan rekaman audio yang diunduh ke galeri perangkat. |
| **`Pengecualian Hemat Baterai`** | Mencegah sistem Android mematikan alarm penjadwal pesan saat HP dalam kondisi layar mati (*Doze mode*). |
| **`Tampilkan di Atas Aplikasi Lain`** | Menampilkan bilah progres unduhan melayang dan layar simulasi panggilan masuk palsu. |

---

## ❓ Tanya Jawab & Solusi Masalah (FAQ)

<details>
<summary><b>Kenapa Google Play Protect memberi peringatan saat install APK?</b></summary>
Peringatan ini normal untuk semua modul Xposed mandiri yang dipasang di luar Play Store. Karena Rhpatch memiliki izin tingkat lanjut (Aksesibilitas & Notifikasi) untuk fitur auto-reply dan penjadwal pesan, algoritma Play Protect otomatis menandainya sebagai aplikasi tidak dikenal. Anda dapat memilih <i>"Tetap Install"</i> dengan aman.
</details>

<details>
<summary><b>Kenapa setelah mengubah pengaturan di Rhpatch, fiturnya belum berubah di aplikasi target?</b></summary>
Aplikasi seperti TikTok, Instagram, dan WhatsApp menyimpan cache antarmuka di memori RAM. Setiap kali Anda mengubah saklar filter di Rhpatch, pastikan lakukan <b>Force Stop (Paksa Berhenti)</b> pada aplikasi target sekali agar perubahan baru diterapkan secara bersih.
</details>

---

## 📄 Lisensi & Hak Cipta
Project ini dilisensikan di bawah [GNU General Public License v3.0](LICENSE). Dibuat untuk tujuan edukasi, privasi, dan kustomisasi pribadi.

<div align="center">
  <i>Dikembangkan dengan dedikasi & ketelitian oleh <b>Rhdevs</b></i>
</div>


