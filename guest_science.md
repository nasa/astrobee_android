# Guest Science Application notes

## Data directories

Guest science applications can generate data that will be stored on the Android
HLP SD card (total size is 64GB, shared with other applications).

The data produced during an experiment can be made available either for:
  - immediate download
  - delayed download

Immediate versus delayed download depends of the amount of data to transfer and
of the payload agreement with ISS.

In addition, Guest Science may need to upload files to Astrobee (in case the
data cannot fit into a generic message). These incoming files will also go
in a specific directory.

### Immediate download

The files scheduled for immediate download are transferred from Astrobee to the
Ground just after the experiment using DTN over Trek.

### Delayed download

The files scheduled for delayed download are copied to the ISS NAS. The NAS
is synched to the ground at ISS convenience.

### Incoming files

Incoming files will be manually copied by the engineering team to Astrobee to
a specific location on the HLP.

### Directory structure

```
/sdcard/data/PAYLOAD/
                    immediate/DATE
                    delayed/DATE
                    incoming
```

  - `PAYLOAD` is the Java package name of the Guest Science application.
  - `DATE` is recommended (but not enforced) and follow the scheme:
    - `YYYY-MM-DD`

It is planned that the `/sdcard/data/PAYLOAD/` path is passed to the
GuestScience Application by the GuestScience Manager using an Intent. Waiting to
have this mechanism in place, the GuestScience Application is advised to code
the data path (according the scheme above) in a way that can accept a path from
an external application later.
