#!/bin/bash

LANG=${LANG=en_US.UTF-8}
declare -A strings

if [[ ${LANG,,} =~ 'ko' ]]; then
  strings=([error]='오류' [warning]='경고'
	[usage]="사용법: $0 <start|stop>"
	[args]="매개변수 1개 필수입니다"
	[no_adb]="adb가 없습니다"
	[cannot_connect]="HLP에 연결할 수는 없습니다"
	[cannot_start]="수집 시작할 수가 없습니다"
	[cannot_stop]="수집 중지할 수가 없습니다"
	[success]="성공"
  )
else
  strings=([error]='error' [warning]='warning'
	[usage]="usage: $0 <start|stop>"
	[args]="must provide an argument"
	[no_adb]="adb not found"
	[cannot_connect]="unable to connect to the HLP"
	[cannot_start]="unable to start capture"
	[cannot_stop]="unable to stop capturing"
	[success]="success"
  )
fi

_lookup() {
  local _out=$1
  local _res=${strings[$2]}
  eval $_out=\$_res
}

call() {
  local s; _lookup s "$2"
  $1 $s
}
