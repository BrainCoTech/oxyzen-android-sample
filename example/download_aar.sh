#!/bin/bash
set -e
cd ${0%/*}
SCRIPT_DIR=$(pwd)

# colorful echo functions
function echo_y() { echo -e "\033[1;33m$@\033[0m" ; }   # yellow
function echo_r() { echo -e "\033[0;31m$@\033[0m" ; }   # red

# liboxyzen settings
LIB_VERSION="1.2.4"
LIB_NAME=""
URL="https://app.brainco.cn/universal/zenlite-sdk-prebuild/android/aar/${LIB_VERSION}/libs.zip"

# check windows
if [[ "$OSTYPE" == "msys" ]]; then
    $COMSPEC /c download-lib.bat
    exit
fi

# 1. check version from VERSION file
if [ -f VERSION ] && grep --fixed-strings --quiet ${LIB_VERSION} VERSION; then
    echo_y "[liboxyzen] liboxyzen (${LIB_VERSION}) is already installed"
    cat VERSION
    exit
fi

# clean files
rm -rf libs

ZIP_NAME="libs.zip"
wget ${URL}
unzip -o $ZIP_NAME -d . > /dev/null
rm $ZIP_NAME
rm -rf __MACOSX

# 4. create VERSION file
echo "liboxyzen Version: ${LIB_VERSION}" >  VERSION
echo "Update Time: $(date)"             >> VERSION

echo_y "[liboxyzen] liboxyzen (${LIB_VERSION}) is downloaded"