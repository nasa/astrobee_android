\defgroup audio_manager_mic_speaker_test Test: Microphone and Speaker Test
\ingroup audio_manager

The microphone and speaker test allows a user to play a sound over the speakers, record a clip of audio, and play the audio back. To run the test program, follow these steps:

On the hlp ...

1. Touch the apps icon on the Android home screen. The apps icon should be a circle with 6 squares in it.

2. Touch the Mic and Sound test app. If the app isn't there, follow the install section.

3. Follow the app.

# Installing the Mic and Sound Test Apk
Copy the apk from Ted's spheresgoat account to the llp

    scp /home/tfmorse/mic_test.apk ubuntu@10.42.0.31

ssh into the llp

    ssh ubuntu@10.42.0.31

Make sure the hlp is connected. The following command lists the android devices connected to the llp. If there are no devices, talk to Ted.

    adb devices

Install apk to the hlp

    adb install -r mic_test.apk
