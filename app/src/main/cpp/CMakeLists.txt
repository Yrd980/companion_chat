cmake_minimum_required(VERSION 3.22.1)

project(companion_llama LANGUAGES C CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CXX_EXTENSIONS OFF)

set(LLAMA_CPP_DIR "${CMAKE_CURRENT_LIST_DIR}/../../../../third_party/llama.cpp")

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
if (NOT DEFINED LLAMA_INSTALL_VERSION)
    set(LLAMA_INSTALL_VERSION "0.0.0")
endif()

find_package(Threads REQUIRED)

set(MTMD_DIR "${LLAMA_CPP_DIR}/tools/mtmd")
add_library(mtmd STATIC
    "${MTMD_DIR}/mtmd.cpp"
    "${MTMD_DIR}/mtmd-audio.cpp"
    "${MTMD_DIR}/mtmd-image.cpp"
    "${MTMD_DIR}/mtmd-helper.cpp"
    "${MTMD_DIR}/clip.cpp"
    "${MTMD_DIR}/models/cogvlm.cpp"
    "${MTMD_DIR}/models/conformer.cpp"
    "${MTMD_DIR}/models/dotsocr.cpp"
    "${MTMD_DIR}/models/gemma4a.cpp"
    "${MTMD_DIR}/models/gemma4v.cpp"
    "${MTMD_DIR}/models/glm4v.cpp"
    "${MTMD_DIR}/models/granite-speech.cpp"
    "${MTMD_DIR}/models/hunyuanocr.cpp"
    "${MTMD_DIR}/models/internvl.cpp"
    "${MTMD_DIR}/models/kimivl.cpp"
    "${MTMD_DIR}/models/kimik25.cpp"
    "${MTMD_DIR}/models/nemotron-v2-vl.cpp"
    "${MTMD_DIR}/models/llama4.cpp"
    "${MTMD_DIR}/models/llava.cpp"
    "${MTMD_DIR}/models/minicpmv.cpp"
    "${MTMD_DIR}/models/paddleocr.cpp"
    "${MTMD_DIR}/models/pixtral.cpp"
    "${MTMD_DIR}/models/qwen2vl.cpp"
    "${MTMD_DIR}/models/qwen3vl.cpp"
    "${MTMD_DIR}/models/qwen3a.cpp"
    "${MTMD_DIR}/models/step3vl.cpp"
    "${MTMD_DIR}/models/siglip.cpp"
    "${MTMD_DIR}/models/whisper-enc.cpp"
    "${MTMD_DIR}/models/deepseekocr.cpp"
    "${MTMD_DIR}/models/mobilenetv5.cpp"
    "${MTMD_DIR}/models/youtuvl.cpp"
    "${MTMD_DIR}/models/yasa2.cpp"
)

target_include_directories(mtmd PUBLIC "${MTMD_DIR}")
target_include_directories(mtmd PRIVATE "${LLAMA_CPP_DIR}" "${LLAMA_CPP_DIR}/vendor")
target_link_libraries(mtmd PUBLIC ggml llama)
target_link_libraries(mtmd PRIVATE Threads::Threads)
target_compile_features(mtmd PRIVATE cxx_std_17)

if (NOT MSVC)
    target_compile_options(mtmd PRIVATE -Wno-cast-qual)
endif()

if (ANDROID)
    target_compile_options(mtmd PRIVATE -Wno-missing-prototypes)
endif()

add_library(companion_llama SHARED llama_jni.cpp)

target_include_directories(companion_llama PRIVATE
    "${LLAMA_CPP_DIR}/include"
    "${LLAMA_CPP_DIR}/tools/mtmd"
)

target_link_libraries(companion_llama
    llama
    mtmd
    android
    log
)
