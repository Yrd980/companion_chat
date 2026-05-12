cmake_minimum_required(VERSION 3.22.1)

project(companion_llama LANGUAGES C CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CXX_EXTENSIONS OFF)

set(LLAMA_CPP_DIR "${CMAKE_CURRENT_LIST_DIR}/../../../../../third_party/llama.cpp")

set(BUILD_SHARED_LIBS OFF CACHE BOOL "Build llama.cpp as static libraries" FORCE)
set(LLAMA_BUILD_COMMON OFF CACHE BOOL "Skip llama.cpp common tools" FORCE)
set(LLAMA_BUILD_TESTS OFF CACHE BOOL "Skip llama.cpp tests" FORCE)
set(LLAMA_BUILD_TOOLS OFF CACHE BOOL "Skip llama.cpp tools" FORCE)
set(LLAMA_BUILD_EXAMPLES OFF CACHE BOOL "Skip llama.cpp examples" FORCE)
set(LLAMA_BUILD_SERVER OFF CACHE BOOL "Skip llama.cpp server" FORCE)
set(GGML_NATIVE OFF CACHE BOOL "Disable host-native flags for Android cross builds" FORCE)
set(GGML_OPENMP OFF CACHE BOOL "Disable OpenMP" FORCE)
set(GGML_BLAS OFF CACHE BOOL "Disable BLAS" FORCE)
set(GGML_LLAMAFILE OFF CACHE BOOL "Disable llamafile" FORCE)

add_subdirectory("${LLAMA_CPP_DIR}" "${CMAKE_CURRENT_BINARY_DIR}/llama.cpp")

add_library(companion_llama SHARED llama_jni.cpp)

target_include_directories(companion_llama PRIVATE
    "${LLAMA_CPP_DIR}/include"
)

target_link_libraries(companion_llama
    llama
    android
    log
)
