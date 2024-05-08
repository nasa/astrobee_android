# HLP Developer Board Installation Instructions

These instructions are addressed to Astrobee users who are interested in Guest
Science development using similar High-Level Processor (HLP) hardware as the 
one used in the robot.

This procedure is a sequence of steps to set up an 
[**Inforce 6640**](https://www.penguinsolutions.com/company/resources/newsroom/inforce-family-inforce-6640)
developer board as a replication of the HLP in Astrobee.

*This procedure requires to install the __Inforce 6601__ version of the Android
 Operating System (OS) on an __Inforce 6640__ board. NASA users can get the OS 
 files from the NASA network. If you are a Non-NASA user please refer to your
 Astrobee point of contact.*

## Developer Board Setup

In this section, you will set up a developer board in preparation for its 
flashing procedure.

### Hardware prerequisites
- Developer board INFORCE 6640 SBC.
- Inforce 6640 12V Power Adapter. Usually comes with the board.
- Micro-USB to USB cable.
- Mini-HDMI to HDMI cable.
- Ethernet cable (RJ-45).
- USB Mouse.
- External monitor HDMI compatible (it will be the screen for the board).

### Unpacking the hardware

Open the box containing the Inforce board and remove it from the plastic bag.
Be careful when you handle this hardware since it is electrostatic sensitive.

If your board comes with a power adapter, remove it from its covering and plug
the two pieces together.

### Preparing board interfaces

On one edge of the board, you will notice a sequence of port interfaces: 
Ethernet (RJ-45), USB, Micro-USB, Audio, Mini-HDMI, Power Supply. 

- Plug your Ethernet cable in the RJ-45 port.
- Plug your USB mouse into the USB port.
- Plug your Micro-USB cable in the Micro-USB port.
- Plug your HDMI cable in the corresponding Mini-HDMI port.
- Take the other extreme of the HDMI cable and plug it in the HDMI port
  in your external monitor.
- Plug the power supply cable in its corresponding port.
- Although you won't see anything yet, power on your external monitor.

**DO NOT power the board on.**

## Host Computer Setup

In this section, you will set up your computer (VM or native) to flash
and test the board.

### Software prerequisites
- Since Astrobee current development is using the 64-bit version of Ubuntu
  20.04, we strongly suggest you install this OS on a host machine or a
  VMWare Virtual Machine. This sequence of steps has only been tested with 
  this Linux distribution.
- Ensure to have at least 4 GB free on disk before starting.
- Ensure you are allowed to edit your network configuration (e.g. be able to 
  change IP address, etc. from your host computer). See the next section for 
  more details.

### Hardware prerequisites
- Make sure you have a **USB port** available in your computer.
- Make sure you have an **Ethernet port** available in your computer.
- If you are not allowed to edit the configuration for the Ethernet port
  on your computer, you will need a 
  [USB - Ethernet adapter](https://www.linksys.com/us/support-product?pid=01t80000003fwbWAAQ)
  so you can modify its settings (if you are not allowed to add new hardware, 
  you still can bridge this adapter to a Virtual Machine and work from there).

### Preparing machine interfaces

Take the other side of the Ethernet cable already plugged in the board, and 
plug it in the Ethernet port in your computer (or in the adapter and then in 
another USB port).

Take the other side of the Micro-USB to USB cable (the USB side) and
plug it in the USB port in your computer. 

*Note you probably won't see any response yet since the board is shut down.*

## Preparation of the disk image for HLP

In this section, you will download the files for the OS installation and 
prepare them for flashing the INFORCE 6640 board from your computer.

### Prerequisites
- NASA Network access.
- The needed files for this procedure are currently stored in the `hivemind`
  server, so you need to have credentials there to download them.
- Ensure you have both _scp_ and _unzip_ commands installed in your computer.

### Prepare files

Create a new directory to store the download files. Then go to it using the 
following commands:

    mkdir $YOUR_PATH/inforce && cd $YOUR_PATH/inforce

*$YOUR_PATH is the path to the directory where you want to download the
 files.*

Create a variable to store the remote path on the `hivemind` server:

    SRCDIR=/home/p-astrobee/astrobee/InForce/6601

Download the _zip_ file using _scp_:

    scp <your_ndc_username>@hivemind:$SRCDIR/Inforce-IFC6601-AndroidBSP-880457-Rel-v2.1.zip .

Unzip the file and change to the 'binaries' directory:

    unzip Inforce-IFC6601-AndroidBSP-880457-Rel-v2.1.zip
    cd Inforce-IFC6601-AndroidBSP-880457-Rel-v2.1/binaries

## Installing Android OS Image

In this section, you will install an image OS in the Inforce board.

### Install required Android dependencies

Ensure you have the **fastboot** and **adb** packages installed in your 
computer. If not, proceed to install them using the following commands:

    sudo apt-get install adb fastboot

### Go into fastboot mode

Ensure the board is off and power connected. 

Then press the power button (it has "PWR" written underneath it) and the VOL-
button next to it **at the same time** and **hold them** at least 5 seconds.

Look at the monitor connected to the board. You should see the Linux Logo 
appear and stay on the screen.

*If the Android system starts, shut the board down and repeat.*

### Flashing the HLP

You must ensure the HLP is detected in your Ubuntu machine (either native or 
VMWare). From your Ubuntu machine execute:

    sudo fastboot devices

*If you are running a VM, ensure the HLP is being detected in the VM not in the
 host machine.*

Ensure you see only one device. If you don't see any, repeat 
'Go into fastboot mode'.

Execute the flashing script inside the 'binaries' directory.

    sudo /bin/sh -e ./flashall.sh

*This will take some time. Be patient. The Inforce board will restart at the
 end.*

## Setting up Ubuntu network

### Ethernet

Go to your network manager and edit the configuration related to the Ethernet
interface connected to the HLP. Set an IP inside the 10.42.0.1/24 subnet. This
will become the LLP IP. For example: `10.42.0.34/24`

### Edit HOSTS file

Open the hosts file with an editor of your election. Add 3 new entries.

For example:

    127.0.0.1   localhost
    127.0.1.1   ubuntu

    10.42.0.36  hlp
    10.42.0.35  mlp
    10.42.0.34  llp

    [...]

## Setting up HLP network

### Ethernet

From your Ubuntu machine, change to your home directory

    cd ~

Create a text file <text_file.sh> and write the following inside:

    #!/bin/sh
    
    set -e
    
    sleep 10
    
    ip addr add 10.42.0.36 dev eth0
    ip link set dev eth0 up
    
    ip rule flush
    ip rule add pref 32766 from all lookup main
    ip rule add pref 32767 from all lookup default

*Replace 10.42.0.36 with the IP of your election if needed*

Push the previous file to the HLP board using the following:

First make sure the adb server is not running:

    adb kill-server

Then start the server with root privileges:

    sudo adb start-server

Finally, push the file by typing the following command:

    adb push <text_file.sh> /sdcard/eth0.sh

*Note: <text_file> represents the name (path if needed) to the file 
 you created before.*

Open an adb shell (a connection between your computer and the HLP board), and
move the file to the 'persist' partition. To do this, type the following
commands from your computer:

    adb shell
    su 0 mv /sdcard/eth0.sh /persist/

Set permissions and ownership for the file using:

    su 0 chmod 755 /persist/eth0.sh
    su 0 chown 0:0 /persist/eth0.sh

Configure ADB to work over Ethernet instead of USB and exit the shell by typing:

    su 0 setprop persist.adb.tcp.port 5555
    exit

### Edit the HLP HOSTS file

Pull the current Android hosts file to your home directory.

    adb pull /system/etc/hosts $HOME

Open the file located in `$HOME/hosts`. Add the following text and save it.
Substitute IPs as needed

    127.0.0.1       localhost
    ::1             ip6-localhost

    10.42.0.36      hlp
    10.42.0.35      mlp
    10.42.0.34      llp

Push the file to the HLP Board.

    adb root    # Wait a few seconds
    adb remount
    adb push ~/hosts /system/etc


## Additional configurations

Increase screen timeout

    adb shell su 0 settings put system screen_off_timeout 1800000

Disable lock screen

    adb shell
    su 0 sqlite3 /data/system/locksettings.db 'update locksettings set value=1 where name="lockscreen.disabled"'
    exit

Reboot to apply changes

    adb reboot


At this point, you should be able to ping the HLP and execute apps in it.

    ping -c4 hlp
    adb shell ping -c4 llp
