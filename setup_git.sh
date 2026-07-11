#!/bin/bash
echo "Initializing Git repository..."
git init

echo "Cleaning existing directories to prevent submodule errors..."
rm -rf app/src/main/cpp/ogg app/src/main/cpp/opus app/src/main/cpp/libopusenc

echo "Adding submodules..."
git submodule add --force https://github.com/xiph/ogg app/src/main/cpp/ogg
git submodule add --force https://github.com/xiph/opus app/src/main/cpp/opus
git submodule add --force https://github.com/xiph/libopusenc app/src/main/cpp/libopusenc

echo "Initializing and updating submodules..."
git submodule update --init --recursive

echo "Adding all files to git..."
git add .

echo "Creating commit..."
git commit -m "Initialize project with submodules, scheduler, auto-reply, and keystore"

echo "Done! You can now run:"
echo "git remote add origin <your-github-repo-url>"
echo "git push -u origin main"
