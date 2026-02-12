#!/bin/bash
set -e

VERSION="2.1.8"
BASE_URL="https://github.com/9seconds/mtg/releases/download/v${VERSION}"

echo "=== Downloading mtg v${VERSION} binaries for Android ==="
echo

mkdir -p app/src/main/jniLibs/{arm64-v8a,armeabi-v7a,x86_64,x86}
mkdir -p temp_downloads

download_and_extract() {
    local arch=$1
    local file=$2
    local target_dir=$3

    echo "Downloading ${file}..."

    if [ -f "temp_downloads/${file}" ]; then
        echo "  Already downloaded, skipping..."
    else
        curl -L -o "temp_downloads/${file}" "${BASE_URL}/${file}"
    fi

    echo "  Extracting to ${target_dir}..."

    rm -rf temp_downloads/mtg*

    tar -xzf "temp_downloads/${file}" -C temp_downloads --strip-components=1

    cp temp_downloads/mtg "app/src/main/jniLibs/${target_dir}/libmtg.so"
    chmod +x "app/src/main/jniLibs/${target_dir}/libmtg.so"
    rm temp_downloads/mtg

    echo "  ✓ ${arch} done"
    echo
}

download_and_extract "arm64-v8a" "mtg-${VERSION}-linux-arm64.tar.gz" "arm64-v8a"
download_and_extract "armeabi-v7a" "mtg-${VERSION}-linux-armv7.tar.gz" "armeabi-v7a"
download_and_extract "x86_64" "mtg-${VERSION}-linux-amd64.tar.gz" "x86_64"
download_and_extract "x86" "mtg-${VERSION}-linux-386.tar.gz" "x86"

echo "Cleaning up..."
rm -rf temp_downloads

echo
echo "=== Download complete! ==="
echo "Binaries are in app/src/main/jniLibs/"
echo
ls -lh app/src/main/jniLibs/*/libmtg.so
echo
du -sh app/src/main/jniLibs/
