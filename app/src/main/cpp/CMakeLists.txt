cmake_minimum_required(VERSION 3.22.1)

project(eteruee_runtime LANGUAGES CXX)

add_library(eteruee_runtime SHARED native_runtime.cpp)

target_compile_features(eteruee_runtime PRIVATE cxx_std_17)
target_compile_options(
    eteruee_runtime
    PRIVATE
    -O3
    -fvisibility=hidden
    -fdata-sections
    -ffunction-sections
    -Wall
    -Wextra
)

target_link_options(
    eteruee_runtime
    PRIVATE
    -Wl,--gc-sections
)
