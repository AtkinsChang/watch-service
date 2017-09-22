cmake_minimum_required(VERSION 3.7 FATAL_ERROR)

project(macosx-watch-service)

macro(add_compiler_flags FLAGS)
    set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} ${FLAGS}")
    set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} ${FLAGS}")
endmacro()

macro(add_linker_flags FLAGS)
    set(CMAKE_EXE_LINKER_FLAGS "${CMAKE_EXE_LINKER_FLAGS} ${FLAGS}")
    set(CMAKE_SHARED_LINKER_FLAGS "${CMAKE_SHARED_LINKER_FLAGS} ${FLAGS}")
endmacro()

function(output_directory TGT DIR)
    set_target_properties(${TGT} PROPERTIES RUNTIME_OUTPUT_DIRECTORY "${CMAKE_BINARY_DIR}/${DIR}")
    set_target_properties(${TGT} PROPERTIES ARCHIVE_OUTPUT_DIRECTORY "${CMAKE_BINARY_DIR}/${DIR}")
    set_target_properties(${TGT} PROPERTIES LIBRARY_OUTPUT_DIRECTORY "${CMAKE_BINARY_DIR}/${DIR}")
endfunction()

# CLFAGS
if(NOT DEFINED COMPILER_SHARED_FLAGS)
    set(COMPILER_SHARED_FLAGS "-g -O3 -Wall -fPIC -flto -fno-omit-frame-pointer")
endif()
add_compiler_flags("${COMPILER_SHARED_FLAGS}")

# LDFLAGS
if(NOT DEFINED LINKER_SHARED_FLAGS)
    set(LINKER_SHARED_FLAGS "-Wl,-undefined,error")
endif()
add_linker_flags("${LINKER_SHARED_FLAGS}")

# JNI
find_package(JNI REQUIRED)
if (JNI_FOUND)
    message (STATUS "JNI_INCLUDE_DIRS=${JNI_INCLUDE_DIRS}")
    message (STATUS "JNI_LIBRARIES=${JNI_LIBRARIES}")
endif()
message (STATUS ${_JAVA_HOME})

find_library(CoreServices CoreServices)

include_directories(
        ${JNI_INCLUDE_DIRS}
)

add_library(mws-jni SHARED
        edu_nccu_plsm_watchservice_MacOSXWatchServiceImpl_Poller.h
        edu_nccu_plsm_watchservice_SunNioFsUnixException.h
        macosx_watch_service.h
        macosx_watch_service.c
        util.c
)

target_link_libraries(mws-jni
        ${CoreServices}
)

if(DEFINED OUTPUT_DIRECTORY)
    message (STATUS "OUTPUT_DIRECTORY=${OUTPUT_DIRECTORY}")
    output_directory(mws OUTPUT_DIRECTORY)
endif()
