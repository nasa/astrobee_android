#!/bin/sh

wd=${wd:-$PWD}

euid=$(id -u)

if [ -t 1 ]; then
  c_reset='\033[0m' c_bold='\033[1m' c_green='\033[32m'
  c_red='\033[31m'  c_yellow='\033[33m' c_blue='\033[34m'
  c_grey='\033[90m'
else
  c_reset='' c_bold='' c_red='' c_yellow='' c_blue='' c_grey='' c_green=''
fi

LL=${LL:-1}

dbg () {
  [ "$LL" -gt 0 ] && return
  printf "${c_grey}[ DEBUG ]${c_reset} %b\\n" "$*" >&2
}

err () {
  printf "${c_red}[ ERROR ]${c_reset} %b\\n" "$*" >&2
}

warn () {
  printf "${c_yellow}[  WARN ]${c_reset} %b\\n" "$*" >&2
}

info () {
  printf "${c_blue}[  INFO ]${c_reset} %b\\n" "$*" >&2
}

fin () {
  printf "${c_green}[  DONE ]${c_reset} %b\\n" "$*" >&2
}

die () {
  printf "${c_red}${c_bold}[ FATAL ]${c_reset} ${c_bold}%b${c_reset}\\n" "$1" >&2
  exit "${2:-1}"
}

usage() {
  local e
  e="$(basename "$0")"

  # yes, I actually know what I'm doing putting a variable
  # in the format string...
  # shellcheck disable=SC2059
  printf "${help:-usage: %s}\\n" "$e" >&2
  exit 1
}

# usage: ensure cmd..
ensure() {
  "${@}" || die "cause of failure: '$*'"
}

# usage: check_req <command> [command2 [command3 ...]]
check_req() {
  local req=''

  [ "$#" -ge 1 ] || die "invalid call to check_req"
  for req in "$@"; do
    command -v "$req" >/dev/null || die "please install $req"
  done
}

isroot() {
  test "$euid" -eq 0
}

# usage: strstr <haystack> <needle>
strstr() {
  [ "$#" -eq "2" ] || die "invalid call to strstr"
  test "${1#*${2}}" != "$1"
}

# usage: fnmatch <glob> <var>
fnmatch() {
  case "$2" in
    $1) return 0 ;;
    *) return 1 ;;
  esac
}

# usage: download <url> <target filename> [checksum|'sha1']
download () {
  local sum='sha1sum'
  local failed=0

  [ "$#" -ge 2 ] || die "invalid call to download"
  command -v wget >/dev/null || die "wget not installed."

  if [ ! -r "$wd/cache/$2" ]; then
    mkdir -p "$wd"/cache

    # Fetch the file
    info "fetching $2"
    if fnmatch "http*" "$1"; then
      (cd "$wd/cache" && wget -q --show-progress --progress=bar -O "$2" "$1") || \
        die "failed to download $2 via wget"
    else
      (cd "$wd/cache" && scp "$1" "$2") || \
        die "failed to download $2 via scp"
    fi
  fi

  if [ -r "$wd/cache/$2" ] && [ -n "$3" ]; then
    if [ "$3" = "sha1" ]; then
      if [ ! -r "$wd/cache/$2.sha1" ]; then
        info "downloading checksum file"
        download "$1.sha1" "$2.sha1" || die "unable to download checksum"
      fi

      info "verifying $sum of $2"
      (cd "$wd"/cache && sha1sum --quiet -c "$wd/cache/$2.sha1") || \
        failed=1
    else
      if [ "${#3}" -eq 32 ]; then
        sum=md5sum
        command -v "$sum" >/dev/null || die "${sum} not available"
      fi

      info "verifying ${sum} of $2"
      echo "$3 *$wd/cache/$2" | $sum -c -w --status || \
        failed=1
    fi

    if [ "$failed" -ne 0 ]; then
      warn "checksum failed, removing download and retrying"
      rm "$wd/cache/$2"
      download "$1" "$2" "$3"
    fi
  fi
}

# get the size of a file (semi-portable)
# usage: fsize <file>
fsize() {
  if command -v stat >/dev/null; then
    { stat -Lc '%s' "$1" 2>/dev/null || \
      stat -Lf '%z' "$1" 2>/dev/null ; } && \
        return
  fi
  wc -c <"$1"
}

# usage: file_newer <f1> <f2>
file_newer () {
  test "$#" -eq 2 || die "usage: file_newer <file1> <file2>" 

  # The source file exist, but the target does not, we need to build it
  ( [ -e "$1" ] && [ ! -e "$2" ] ) && return 0

  # The destination exists, but there is no source, assume they have an
  # existing file they want to use.
  ( [ ! -e "$1" ] && [ -e "$2" ] ) && {
    warn "$1 does not exist, but $2 does, assuming bootstrap situation"
    return 1
  }

  # Both exist, check timestamps
  test "$1" -nt "$2"
}

# usage: dir_newer <dir> <file>
dir_newer() {
  test "$#" -eq 2 || die "usage: dir_newer <dir> <file>" 

  # The source file exists, but the target does not, we need to build it
  ( [ -e "$1" ] && [ ! -e "$2" ] ) && {
    dbg "$2 does not exist"
    return 0
  }

  # The target exists, but there is no source dir, assume they have an
  # existing file they want to use.
  ( [ ! -e "$1" ] && [ -e "$2" ] ) && {
    warn "$1 does not exist, but $2 does, assuming bootstrap situation"
    return 1
  }

  # Find at least one file that is newer 
  [ "$(find "$1" -newer "$2" -print -quit | wc -l)" -gt 0 ]
}

_def_trap_=''
_curr_trap_="$_def_trap_"
trp() {
  [ "$#" -ge 1 ] || die "usage: trp <action> <trap ...>"

  act=$1; shift

  case "$act" in
    add|app*) _curr_trap_="$_curr_trap_ $*;" ;;
    pre*)     _curr_trap_="$*; $_curr_trap_" ;;
    reset)    _curr_trap_="$_def_trap_" ;;
    *)        error "unknown arg $act, ignoring" ;;
  esac

  # shellcheck disable=SC2064
  trap "${_curr_trap_}" 0 HUP QUIT ILL ABRT FPE SEGV PIPE TERM
}
