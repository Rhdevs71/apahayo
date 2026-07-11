@echo off
echo Initializing Git repository...
git init

echo Cleaning existing directories to prevent submodule errors...
rd /s /q app\src\main\cpp\ogg 2>nul
rd /s /q app\src\main\cpp\opus 2>nul
rd /s /q app\src\main\cpp\libopusenc 2>nul

echo Adding submodules...
git submodule add --force https://github.com/xiph/ogg app/src/main/cpp/ogg
git submodule add --force https://github.com/xiph/opus app/src/main/cpp/opus
git submodule add --force https://github.com/xiph/libopusenc app/src/main/cpp/libopusenc

echo Initializing and updating submodules...
git submodule update --init --recursive

echo Adding all files to git...
git add .

echo Creating commit...
git commit -m "Initialize project with submodules, scheduler, auto-reply, and keystore"

echo.
echo Done! You can now run:
echo git remote add origin https://github.com/Rhdevs71/apahayo
echo git push -u origin main
echo.
pause
