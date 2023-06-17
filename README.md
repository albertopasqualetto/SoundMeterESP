# SoundMeterESP   <img src="https://github.com/albertopasqualetto/SoundMeterESP/assets/39854348/61a3768c-4095-48b0-b03a-72d4ea1ec5c0" alt="App icon" align="right" width="128" />

This is a simple sound meter Android app created for the Embedded Systems Programming course (Computer Engineering) at the University of Padua.

## Description

It uses the internal microphone of the device to measure the sound level in decibels.

The sound level is displayed in a chart and in a text view in "real time", also there is the history of the sound level of the last 5 minutes.

## Technical details

The app is written in Kotlin and uses the [MPAndroidChart](https://github.com/PhilJay/MPAndroidChart) library to display the chart.

The app is based on Jetpack Compose for showing the UI and it follows material3 guidelines and style as much as possible.

Its `targetSdk` is API 31 (Android 12) since my device is running Android 12 (Samsung Galaxy S10) and the `minSdk` is API 26. 
It implements the dynamic color theming introduced in Android 12 both in icon and in the app's colors.

The app uses a foreground service to keep the microphone active even when the app is in background so that it can keep measuring the sound level.

The dB value presented is not accurate since it is not calibrated with a reference of 0 dB and the `MediaRecorder.AudioSource.MIC` does some elaboration to the audio (`MediaRecorder.AudioSource.UNPROCESSED` is not used since it is not supported on all the devices); but it is enough to see the difference between a quiet and a loud environment.

Some Google's libraries are used to get the screen height on rotation in order to choose the right screen layout ([material3-window-size-class](https://developer.android.com/reference/kotlin/androidx/compose/material3/windowsizeclass/package-summary)) and for asking and managing the permissions at runtime easily in compose ([accompanist-permissions](https://google.github.io/accompanist/permissions/)) which relies on the androidx's APIs.

## Screenshots

Horizontal layout in light mode:
<p float="left">
  <img src="https://github.com/albertopasqualetto/SoundMeterESP/assets/39854348/4fe2afa0-f7c6-4ff9-a9fc-928e8ebb2186" alt="Horizontal light mode last second view" width="400" />
  <img src="https://github.com/albertopasqualetto/SoundMeterESP/assets/39854348/6144a309-5b99-44c0-b67c-d9e2228ef285" alt="Horizontal light mode last five minutes view" width="400" /> 
</p>

Vertical layout in dark mode:
<p float="left">
  <img src="https://github.com/albertopasqualetto/SoundMeterESP/assets/39854348/1847befd-18a1-458f-8bc3-53aed4cc5dcb" alt="Vertical dark mode last second view" width="200" />
  <img src="https://github.com/albertopasqualetto/SoundMeterESP/assets/39854348/8c8c0bd1-dc61-4bcf-8d44-d835ab6fd69d" alt="Vertical dark mode last five minutes view" width="200" /> 
</p>
