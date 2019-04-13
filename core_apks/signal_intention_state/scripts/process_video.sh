#!/bin/bash

# A simple script that can be used to handle multiple files ffmpeg executions

# They have to be existing folders
input_folder="/home/rgarciar/Videos/eyes_astrobee_animation/video_processing/to_be_processed/"
input_videos="*"

exit_folder="/home/rgarciar/Videos/eyes_astrobee_animation/video_processing/processed/"
exit_extention=".mp4"

for videofile in $input_folder$input_videos; do
  # Get the filename and extention
  # echo ${videofile##*/}

  # Get just the file name
  base_name=$(basename -- "$videofile")
  base_no_ext_name="${base_name%.*}"
  
  # -------------------------------------- some useful commands ------------------------------  

  # (1) It provides a super fast bitrated video so android video view can use "seek to" function
  # ffmpeg -i $videofile -c:v libx264 -preset superfast -x264opts keyint=2 -acodec copy -f mp4 $exit_folder$base_no_ext_name$exit_extention

  # (2) Converts video file to mp4 format with 1280x720 size, 30 frames rate and 2000 MegaBits bitrate
  # ffmpeg -y -i $videofile -s 1280x720 -r 30 -b:v 2000M $exit_folder$base_no_ext_name$exit_extention

  # (3) Crops (width) the video 1 pixel (to delete gray line some videos have on the edge)
  # ffmpeg -i $videofile -filter:v "crop=iw-1" -c:a copy $exit_folder$base_no_ext_name$exit_extention 

  # (4) Does 1 and 2 in one step
  # ffmpeg -i $videofile -s 1280x720 -r 30 -b:v 2000M -c:v libx264 -preset superfast -x264opts keyint=2 -acodec copy -f mp4 $exit_folder$base_no_ext_name$exit_extention

  # ----------------------------------- Put here the command you want to execute  -------------------------
  ffmpeg -i $videofile -s 1280x720 -r 30 -b:v 2000M -c:v libx264 -preset superfast -x264opts keyint=2 -acodec copy -f mp4 $exit_folder$base_no_ext_name$exit_extention
done;

