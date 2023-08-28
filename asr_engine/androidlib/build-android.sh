dir=$PWD/build

mkdir -p $dir
cd $dir

cmake -DCMAKE_BUILD_TYPE=debug \
    -DCMAKE_TOOLCHAIN_FILE="/home/sunyujia/Android/Ndk/android-ndk-r21e/build/cmake/android.toolchain.cmake" \
    -DANDROID_ABI="arm64-v8a" \
    -DANDROID_PLATFORM="android-21" \
    -DCMAKE_INSTALL_PREFIX=./install ..

make -j4
