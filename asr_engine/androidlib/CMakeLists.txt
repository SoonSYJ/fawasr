cmake_minimum_required(VERSION 3.16)

project(FunASRWebscoket) 

set(CMAKE_CXX_STANDARD 14 CACHE STRING "The C++ version to be used.")
set(CMAKE_POSITION_INDEPENDENT_CODE ON)
set(CMAKE_RUNTIME_OUTPUT_DIRECTORY ${CMAKE_BINARY_DIR}/bin)

set(ONNXRUNTIME_DIR /home/sunyujia/cpp_ws/onnxruntime-android-1.14.0)
 
link_directories(${ONNXRUNTIME_DIR}/lib)
include_directories(${ONNXRUNTIME_DIR}/include)

include_directories(${PROJECT_SOURCE_DIR}/../onnxruntime/include/)
include_directories(${PROJECT_SOURCE_DIR}/../onnxruntime/third_party/yaml-cpp/include/)
include_directories(${PROJECT_SOURCE_DIR}/../onnxruntime/third_party/kaldi-native-fbank)
include_directories(${PROJECT_SOURCE_DIR}/../onnxruntime/src)

add_subdirectory(${PROJECT_SOURCE_DIR}/../onnxruntime/third_party/yaml-cpp yaml-cpp)
add_subdirectory(${PROJECT_SOURCE_DIR}/../onnxruntime/third_party/kaldi-native-fbank/kaldi-native-fbank/csrc csrc)
add_subdirectory(${PROJECT_SOURCE_DIR}/../onnxruntime/src src)

include_directories(${PROJECT_SOURCE_DIR}/../onnxruntime/third_party/glog)
set(BUILD_TESTING OFF)
add_subdirectory(${PROJECT_SOURCE_DIR}/../onnxruntime/third_party/glog glog)

aux_source_directory(${PROJECT_SOURCE_DIR}/../onnxruntime/src src_files)
add_library(fawasr-jni SHARED fawasr.cc ${src_files})

target_link_libraries(fawasr-jni PUBLIC onnxruntime -pthread yaml-cpp csrc glog log)
