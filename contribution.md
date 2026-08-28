# Panduan Kontribusi Rhpatch

Terima kasih atas minat Anda untuk berkontribusi pada proyek open-source Rhpatch! Kami menyambut segala bentuk kontribusi, termasuk perbaikan bug, penambahan fitur baru, serta pelaporan masalah (issues).

## Cara Berkontribusi

1. **Fork Repositori**: Klik tombol `Fork` di pojok kanan atas repositori ini untuk menyalinnya ke akun GitHub Anda.
2. **Kloning Proyek**: Kloning repositori hasil fork ke komputer lokal Anda.
   ```bash
   git clone https://github.com/UsernameAnda/apahayo.git
   ```
3. **Buat Branch Baru**: Sangat disarankan untuk membuat *branch* baru untuk setiap fitur atau perbaikan bug.
   ```bash
   git checkout -b fitur-baru-atau-bugfix
   ```
4. **Mulai Memprogram**: Lakukan perubahan yang diperlukan menggunakan IDE pilihan Anda (seperti Android Studio). Pastikan Anda mengikuti standar kode yang sudah ada dalam proyek ini.
5. **Uji Kode Anda**: Selalu *compile* dan uji coba modul (di *emulator* atau perangkat nyata) sebelum membuat *commit*.
6. **Commit Perubahan**: Pesan commit harus jelas dan mendeskripsikan apa yang diubah.
   ```bash
   git commit -m "feat: Menambahkan dukungan fitur balasan acak"
   ```
7. **Push ke GitHub**:
   ```bash
   git push origin fitur-baru-atau-bugfix
   ```
8. **Buat Pull Request**: Buka repositori utama di GitHub, lalu klik tab `Pull Requests` dan pilih `New Pull Request`. Jelaskan perubahan yang Anda buat secara detail agar mudah di-review oleh *maintainer*.

## Pelaporan Masalah (Issues)
Jika Anda menemukan *bug* atau ingin menyarankan fitur baru, jangan ragu untuk membuat *issue*.
- Gunakan fitur pencarian (Search) di tab Issues untuk memastikan masalah Anda belum pernah dilaporkan.
- Sertakan *log* Xposed/LSPosed dan versi Android/Aplikasi untuk membantu kami melakukan investigasi.
- Jelaskan langkah-langkah untuk mereproduksi *bug* tersebut (How to reproduce).

Terima kasih karena telah menjadikan Rhpatch lebih baik!
