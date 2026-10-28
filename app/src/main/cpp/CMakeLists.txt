cmake_minimum_required(VERSION 3.22.1)
project(whisperjni C CXX)

# whisper.cpp is een git-submodule in third_party/whisper.cpp (git submodule update --init).
if(NOT DEFINED WHISPER_DIR)
    set(WHISPER_DIR ${CMAKE_CURRENT_SOURCE_DIR}/../../../../third_party/whisper.cpp)
endif()

set(BUILD_SHARED_LIBS OFF CACHE BOOL "" FORCE)
set(WHISPER_BUILD_TESTS OFF CACHE BOOL "" FORCE)
set(WHISPER_BUILD_EXAMPLES OFF CACHE BOOL "" FORCE)
set(WHISPER_BUILD_SERVER OFF CACHE BOOL "" FORCE)
set(GGML_NATIVE OFF CACHE BOOL "" FORCE)
set(GGML_OPENMP OFF CACHE BOOL "" FORCE)

# Telefoons van na ~2018 ondersteunen ARMv8.2 met dotprod/fp16; dat maakt transcriptie flink sneller.
if(ANDROID_ABI STREQUAL "arm64-v8a")
    set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -march=armv8.2-a+dotprod+fp16")
    set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -march=armv8.2-a+dotprod+fp16")
endif()

add_subdirectory(${WHISPER_DIR} whisper-build)

add_library(whisperjni SHARED whisper_jni.cpp)
target_link_libraries(whisperjni PRIVATE whisper log)
