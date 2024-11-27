cmake_minimum_required(VERSION 3.4.1)
project(FairyStockfish)

# Set C++ standard
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# Architecture-specific flags
if(${ANDROID_ABI} STREQUAL "x86_64")
    set(ARCH_FLAGS
            "-DIS_64BIT"
            "-DUSE_SSE41"
            "-DUSE_POPCNT"
            "-msse4.1"
            "-mpopcnt"
    )
elseif(${ANDROID_ABI} STREQUAL "arm64-v8a")
    set(ARCH_FLAGS
            "-DIS_64BIT"
            "-DUSE_NEON"
            "-DUSE_POPCNT"
    )
endif()

# Add Fairy-Stockfish source files
file(GLOB STOCKFISH_SOURCES
        ${CMAKE_CURRENT_SOURCE_DIR}/fairy-stockfish/src/*.cpp
        ${CMAKE_CURRENT_SOURCE_DIR}/fairy-stockfish/src/nnue/*.cpp  # Add NNUE source files
        ${CMAKE_CURRENT_SOURCE_DIR}/fairy-stockfish/src/syzygy/*.cpp  # Add Syzygy tablebase source files
        ${CMAKE_CURRENT_SOURCE_DIR}/fairy-stockfish/src/nnue/features/*.cpp  # Add NNUE feature transformer source files
)

# Remove interface-specific files (for python and javascript interface)
list(REMOVE_ITEM STOCKFISH_SOURCES
        ${CMAKE_CURRENT_SOURCE_DIR}/fairy-stockfish/src/ffishjs.cpp
        ${CMAKE_CURRENT_SOURCE_DIR}/fairy-stockfish/src/wasm.cpp
        ${CMAKE_CURRENT_SOURCE_DIR}/fairy-stockfish/src/pyffish.cpp
        ${CMAKE_CURRENT_SOURCE_DIR}/fairy-stockfish/src/pythoncompat.cpp
)

add_library(stockfish SHARED
        stockfish-wrapper.cpp
        ${STOCKFISH_SOURCES}
)

# Include directories
target_include_directories(stockfish PRIVATE
        ${CMAKE_CURRENT_SOURCE_DIR}
        ${CMAKE_CURRENT_SOURCE_DIR}/fairy-stockfish/src
        ${CMAKE_CURRENT_SOURCE_DIR}/fairy-stockfish/src/nnue  # Add NNUE include directory
)

# Add compiler definitions
target_compile_definitions(stockfish PRIVATE
        IS_64BIT
        USE_PTHREADS
        USE_LARGE_PAGES
        VARIANT_FAIRY
)

# Add architecture-specific flags
target_compile_options(stockfish PRIVATE ${ARCH_FLAGS})

# Link required libraries
find_library(log-lib log)
target_link_libraries(stockfish
        ${log-lib}
        android
        atomic
)