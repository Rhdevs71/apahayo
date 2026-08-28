param (
    [string]$Changelog = "Pembaruan minor dan perbaikan bug."
)

Write-Host "Memulai kompilasi APK..." -ForegroundColor Cyan
.\gradlew assembleRelease

if ($LASTEXITCODE -ne 0) {
    Write-Host "Kompilasi gagal! APK tidak dikirim." -ForegroundColor Red
    exit $LASTEXITCODE
}

$ApkFiles = Get-ChildItem -Path "app\build\outputs\apk\release\" -Filter "*.apk"
if ($ApkFiles.Count -eq 0) {
    Write-Host "APK tidak ditemukan di folder output." -ForegroundColor Red
    exit 1
}

$ApkPath = $ApkFiles[0].FullName
Write-Host "Kompilasi sukses! Mengirim APK ($($ApkFiles[0].Name)) ke Telegram..." -ForegroundColor Cyan

$BotToken = "8976405279:AAEg1lt_btbscr8BlDG2pJ0bL-2fX6PuDmk"
$ChatId = "1374922202"
$Caption = "🚀 *Rhpatch v1.5.7.2 Build Sukses!*

*Changelog:*
$Changelog"

curl.exe -F "chat_id=$ChatId" -F "document=@$ApkPath" -F "caption=$Caption" -F "parse_mode=Markdown" "https://api.telegram.org/bot$BotToken/sendDocument"

Write-Host "
Selesai!" -ForegroundColor Green
