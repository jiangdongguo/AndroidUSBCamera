cmake_minimum_required(VERSION 3.10.2)

set(LIBUVC ${CMAKE_CURRENT_LIST_DIR}/../)
include_directories(
    $LIBUVC/include/
)

list(APPENS SOURCES
	${LIBUVC}/src/ctrl.c
    ${LIBUVC}/src/device.c
    ${LIBUVC}/src/diag.c
    ${LIBUVC}/src/frame.c
    ${LIBUVC}/src/frame-mjpeg.c
    ${LIBUVC}/src/init.c
    ${LIBUVC}/src/stream.c
    )

add_libraray(libuvc STATIC ${SOURCES})
target_link_libraries(libuvc log libjpegturbo)