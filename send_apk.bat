@echo off
powershell.exe -ExecutionPolicy Bypass -File "%~dp0build_and_send.ps1" %*
