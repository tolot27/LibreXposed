# LibreExposed
Instructions on how to set a system that will allow open source projects to use
LibreLink algorithm.

# Introduction

Many users of libre have been suffering from the missing of alerts. Many open
source projects have solved this issue but have failed to repeat the official
abbott glucose numbers.

Other users who wanted to use LibreLink with sensors that they bought from
abbott have found that their sensors are not supported.

In this readme, I'll explain how to solve these two issues:

# Solution overview
Exposed
(https://forum.xda-developers.com/xposed/xposed-installer-versions-changelog-t2714053, https://www.xda-developers.com/making-your-own-xposed-modules-is-easier-than-you-think/)
Is a general library that can be installed on every (rooted) android phone. It
allows one, to modify existing apks without that the built in protection of
librelink will recognize it. As a result, it allows one to be able to call the
existing native library and get the same bg results. (Expose can do this,
because it only changes things in memory, not on the disks).
Because of this we can by now, run librelink, and change the data that
identifies the sensor, and by this allow it to work on any sensor.

We should write a broadcastreceiver that will receive the nfc scanned data and
will send it to the native lib to get the bg. This BG should than be sent to
other applications.
A phone that has this sw installed can calculate the BG of more than one person.

# Installation
Install xposed (for example from
https://forum.xda-developers.com/showthread.php?t=3034811). Then install the apk
created by this repository. 
From the list of xposed modules, choose modules and select the apk that was just
installed.
Reboot the phone, LibreLink will now scan sensors from any place in the world.
