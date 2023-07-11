add_definitions(-DMEETING_SCENE)
file (GLOB_RECURSE SOURCE  
${PORJECT_ROOT_PATH}/meeting/*.cc 
${PORJECT_ROOT_PATH}/meeting/*.h 
${PORJECT_ROOT_PATH}/meeting/*.ui)

set(PROJECT_SRC 
	${PROJECT_SRC}
	${SOURCE}
)

set(PROJECT_QRC 
${PROJECT_QRC}
${PORJECT_ROOT_PATH}/meeting/resource/meeting_resource.qrc
)

