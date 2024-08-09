cmake_minimum_required(VERSION 3.22.1)

Project("terminal")

add_library(terminal SHARED
    terminal.c
    )

target_link_libraries(
    terminal
    android
    log
    )
             