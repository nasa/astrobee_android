project (picoflexx)

catkin_package()

install(
  DIRECTORY ${CMAKE_CURRENT_SOURCE_DIR}/scripts
  DESTINATION ${CATKIN_PACKAGE_SHARE_DESTINATION}
  USE_SOURCE_PERMISSIONS
)

install_launch_files()
