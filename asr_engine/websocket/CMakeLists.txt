cmake_minimum_required(VERSION 3.16)

project(FunASRWebscoket) 

set(CMAKE_CXX_STANDARD 14 CACHE STRING "The C++ version to be used.")
set(CMAKE_POSITION_INDEPENDENT_CODE ON)
set(CMAKE_RUNTIME_OUTPUT_DIRECTORY ${CMAKE_BINARY_DIR}/bin)

set(ONNXRUNTIME_DIR /home/sunyujia/cpp_ws/onnxruntime-linux-x64-1.14.0)
set(ONNXRUNTIME_ANDROID_DIR /home/sunyujia/cpp_ws/onnxruntime-android-1.14.0/jni/arm64-v8a)
# set(FFMPEG_DIR /home/sunyujia/cpp_ws/ffmpeg-N-111383-g20b8688092-linux64-gpl-shared)

option(ENABLE_WEBSOCKET "Whether to build websocket server" ON)
 
if(ENABLE_WEBSOCKET)
  # cmake_policy(SET CMP0135 NEW)
  include(FetchContent)
  FetchContent_Declare(websocketpp
  GIT_REPOSITORY https://github.com/zaphoyd/websocketpp.git
    GIT_TAG 0.8.2
    SOURCE_DIR ${PROJECT_SOURCE_DIR}/third_party/websocket
    )
  
  FetchContent_MakeAvailable(websocketpp)
  include_directories(${PROJECT_SOURCE_DIR}/third_party/websocket)
   
  FetchContent_Declare(asio
     URL   https://github.com/chriskohlhoff/asio/archive/refs/tags/asio-1-24-0.tar.gz
   SOURCE_DIR ${PROJECT_SOURCE_DIR}/third_party/asio
  )
  
  FetchContent_MakeAvailable(asio)
  include_directories(${PROJECT_SOURCE_DIR}/third_party/asio/asio/include)
 
  FetchContent_Declare(json
     URL   https://github.com/nlohmann/json/archive/refs/tags/v3.11.2.tar.gz
   SOURCE_DIR ${PROJECT_SOURCE_DIR}/third_party/json
  )
  
  FetchContent_MakeAvailable(json)
  include_directories(${PROJECT_SOURCE_DIR}/third_party/json/include)

  # Include generated *.pb.h files
  link_directories(${ONNXRUNTIME_DIR}/lib)
  # link_directories(${FFMPEG_DIR}/lib)

  include_directories(${PROJECT_SOURCE_DIR}/../onnxruntime/include/)
  include_directories(${PROJECT_SOURCE_DIR}/../onnxruntime/third_party/yaml-cpp/include/)
  include_directories(${PROJECT_SOURCE_DIR}/../onnxruntime/third_party/kaldi-native-fbank)

  add_subdirectory(${PROJECT_SOURCE_DIR}/../onnxruntime/third_party/yaml-cpp yaml-cpp)
  add_subdirectory(${PROJECT_SOURCE_DIR}/../onnxruntime/third_party/kaldi-native-fbank/kaldi-native-fbank/csrc csrc)
  add_subdirectory(${PROJECT_SOURCE_DIR}/../onnxruntime/src src)

  include_directories(${PROJECT_SOURCE_DIR}/../onnxruntime/third_party/glog)
  set(BUILD_TESTING OFF)
  add_subdirectory(${PROJECT_SOURCE_DIR}/../onnxruntime/third_party/glog glog)

  # install openssl first apt-get install libssl-dev
  find_package(OpenSSL REQUIRED)

  add_executable(funasr-wss-server "funasr-wss-server.cpp" "websocket-server.cpp")
  add_executable(funasr-wss-server-2pass "funasr-wss-server-2pass.cpp" "websocket-server-2pass.cpp")
  add_executable(funasr-wss-client "funasr-wss-client.cpp")
  add_executable(funasr-wss-client-2pass "funasr-wss-client-2pass.cpp")

  target_link_libraries(funasr-wss-client PUBLIC funasr ssl crypto rt)
  target_link_libraries(funasr-wss-client-2pass PUBLIC funasr ssl crypto rt)
  target_link_libraries(funasr-wss-server PUBLIC funasr ssl crypto rt)
  target_link_libraries(funasr-wss-server-2pass PUBLIC funasr ssl crypto rt)
else()
  
endif()

