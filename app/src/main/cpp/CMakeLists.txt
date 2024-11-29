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

# Add JFFish source files
file(GLOB STOCKFISH_SOURCES
        ${CMAKE_CURRENT_SOURCE_DIR}/JFFish/src/*.cpp
        ${CMAKE_CURRENT_SOURCE_DIR}/JFFish/src/nnue/*.cpp  # Add NNUE source files
        ${CMAKE_CURRENT_SOURCE_DIR}/JFFish/src/syzygy/*.cpp  # Add Syzygy tablebase source files
        ${CMAKE_CURRENT_SOURCE_DIR}/JFFish/src/nnue/features/*.cpp  # Add NNUE feature transformer source files
)

# Remove interface-specific files (for python and javascript interface)
list(REMOVE_ITEM STOCKFISH_SOURCES
        ${CMAKE_CURRENT_SOURCE_DIR}/JFFish/src/ffishjs.cpp
        ${CMAKE_CURRENT_SOURCE_DIR}/JFFish/src/wasm.cpp
        ${CMAKE_CURRENT_SOURCE_DIR}/JFFish/src/pythoncompat.cpp
        ${CMAKE_CURRENT_SOURCE_DIR}/JFFish/src/pyffish.cpp
)

# add our wrapper as library
add_library(stockfish SHARED
        JFFish/src/jffish.cpp
        ${STOCKFISH_SOURCES}
)

# Include directories
target_include_directories(stockfish PRIVATE
        ${CMAKE_CURRENT_SOURCE_DIR}
        ${CMAKE_CURRENT_SOURCE_DIR}/JFFish/src
        ${CMAKE_CURRENT_SOURCE_DIR}/JFFish/src/nnue  # Add NNUE include directory
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
target_link_libraries(
        stockfish
        android
        atomic
)

