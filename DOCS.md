# AI_AGENT_CODEBASE_GUIDE

## 1. Identitas Project
- **Nama Project**: Rhpatch (Rhpatch)
- **Jenis**: LSPosed / Xposed Module + Host App (Standalone App)
- **Tujuan**: Memodifikasi aplikasi target (WhatsApp, TikTok, YouTube (Morphe), Instagram (Piko)) di tingkat runtime memory lewat LSPosed, serta menyediakan Host App untuk setting, log root, automasi (Accessibility Service).
- **Build System**: Menggunakan Gradle. Proses build otomatis dijalankan melalui script send_apk.bat yang juga berfungsi mengirim APK ke Telegram bot pengguna.

## 2. Arsitektur Dual-Layer
Project ini terbagi menjadi dua dunia yang terisolasi secara default:
1. **Host App**: Berjalan di proses mandiri (com.rhdevs.rhpatch). Menangani UI Settings pengguna, background scheduler, cek root, dan layanan aksesibilitas.
2. **Injected Module**: Berjalan di dalam proses aplikasi target (contoh: com.whatsapp, com.ss.android.ugc.trill). Kelas Xposed (WppXposed.kt) bertindak sebagai gerbang (entry point). 

*PENTING*: Modul tidak bisa langsung mengakses SharedPreferences milik Host tanpa menggunakan skema XSharedPreferences atau ContentProvider.

## 3. Peta Direktori Keseluruhan (Detail)

### Direktori Utama: pp\src\main\

#### A. AndroidManifest.xml
Titik awal konfigurasi Android. Mendaftarkan:
- SettingsActivity (Main UI).
- Meta-data xposedmodule, xposeddescription, xposedminversion.
- AutoSenderAccessibilityService untuk automasi tap (Scheduler).
- Izin root dan boot.

#### B. java\com\rhdevs\rhpatch\
- **Patch.kt & WppXposed.kt**: Titik masuk Xposed (Entry Point). WppXposed akan mendeteksi nama paket aplikasi saat runtime (misal com.whatsapp atau com.ss.android.ugc.trill) dan meload loader yang sesuai.

##### ctivity\
- SettingsActivity.kt: Layar utama Host App. Menampilkan tombol Scheduler, Log Diagnostics, dan informasi versi. Menghandle request Root.

##### dapter\
- LogLineAdapter.kt: Adapter untuk RecyclerView di UI Log Root.
- SettingsAdapter.kt: Adapter umum untuk UI setting.

##### ot\
- TelegramReporter.kt dll: Modul crash reporter, mengirim log error langsung ke grup Telegram khusus jika terjadi crash di modul.

##### data\ & database\
- Menyimpan struktur data lokal. Terdapat AutoReplyContract.kt atau konfigurasi untuk fitur Balas Otomatis.

##### instagram\ (Piko)
- Modul patch untuk Instagram.
- Berisi injeksi UI (Rhpatch Settings di IG), fitur Ghost Mode, Hide Ads, dan fungsi developer.
- Menyuntik ke com.instagram.android.

##### models\
- Berisi class data Kotlin (ScheduledMessage.kt, dll) untuk serialisasi JSON dan database.

##### 
eceivers\
- Broadcast receiver untuk menangkap trigger (seperti boot device atau alarm scheduler).

##### services\
- AutoSenderAccessibilityService.kt: Layanan Aksesibilitas Android. Digunakan oleh Scheduler untuk mengetik dan mengirim pesan secara otomatis ke WhatsApp tanpa root (lewat UI node manipulation).
- UniversalScheduler.kt: Engine penjadwal pesan menggunakan AlarmManager.

##### 	iktok\ (Morphe TikTok)
- Modul patch untuk TikTok.
- Mengatur spoofing negara (Sim Country), UI Feed bypass, Download video tanpa watermark, hapus ads, dan UI injeksi di menu TikTok (TikTokSettingsActivity.kt).

##### ui\ & ui\fragments\
- UI dari Host App. 
- ScreenLockConfigActivity.kt: Konfigurasi gembok modul WhatsApp.
- SchedulerSettingsFragment.kt: UI Setting penjadwal pesan (waktu, penerima, isi pesan).
- HomeFragment.java/.kt, CustomizationFragment, ModulesFragment: Menampilkan layout UI di dalam Host App.

##### utils\
- Kumpulan alat statik (Helper).
- RootDiagnostics.kt: Sistem eksekusi Shell Su untuk membaca logcat logcat atau mendeteksi error Xposed framework.
- PermissionWizardHelper.kt: Mengecek status layanan aksesibilitas dan root manager.
- ConfigManager.kt, Constants.kt.

##### xposed\ (Core WhatsApp Patch)
- Inti dari modul WhatsApp.
- WppCore.kt: Engine hook utama untuk WhatsApp.
- features\: Folder berisi setiap fitur spesifik:
  - chats\: Modifikasi obrolan (Hide Read, Anti-Delete, dll).
  - status\: Modifikasi status (Save Status, dll).
  - calls\: Modifikasi panggilan.
  - security\: Gembok kunci layar (Screen Lock WA).

##### youtube\ (Morphe YouTube)
- Modul patch untuk YouTube (NexAlloy port).
- YoutubeHook.kt: Entry point untuk YT.
- Sub-package: d, layout, ideo, misc (berisi class HideAdsPatch.kt, BackgroundPlaybackPatch.kt, dll).

#### C. 
es\ (Resources)
- layout\: Layout XML (contoh: ctivity_main.xml, ragment_scheduler_settings.xml, dialog_diagnostics_log.xml). Perlu sinkron dengan class UI di Java/Kotlin.
- xml\: File XML khusus seperti PreferenceScreen (ragment_general.xml), konfigurasi aksesibilitas (ccessibility_service_config.xml), dan konfigurasi file paths.
- alues\: Berisi strings.xml (daftar teks bahasa), colors.xml, 	hemes.xml.

## 4. Instruksi Khusus Agent
1. **Dilarang Menambahkan Library yang Tidak Perlu**: Build harus ringan.
2. **Perhatikan XML & Programmatic UI**: Dalam 	twork, UI Host App sebagian adalah Programmatic, dan sebagian XML. Jika mengopi file .kt yang melakukan setContentView(R.layout...), pastikan file XML terkait juga di-copy.
3. **Penggunaan Background Tasks**: Agent tidak boleh menggunakan schedule (polling/timer) atau sleep loop. Agent harus langsung bekerja.
4. **Respek pada "No Mistake" Rule**: Jangan memodifikasi bagian 	iktok atau instagram jika tugasnya adalah memperbaiki Scheduler. Jangan campur-aduk file.

---
*Dokumen ini dibuat otomatis sebagai referensi wajib untuk seluruh operasi pengembangan (Agent AI).*
