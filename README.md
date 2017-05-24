# DataUsageNotifier
![Launcher icon](https://raw.githubusercontent.com/mdkim/DataUsageNotifier/master/app/src/main/res/mipmap-xxxhdpi/ic_launcher.png)
### **Data Usage Notifier - An Android App**

**_Who has been sending/receiving data on your phone in the background?_**

Starting the service gets network activity stats since the last update, and polls every 8 seconds until stopped. Works in the background.

Notification and toasts with updated stats while service is running. Pressing the notification shows a scrolling log of stats.

(Log views are cached on stopping app and restored on start.)

UPDATES:

* 2017-05-21: Added feature to export logs as html; created launcher icon; other icons
* 2017-05-24: Added application icons of logged services in margin; added support for icons in cache serialization
---

<br>

![Log viewer screenshot](http://i.imgur.com/PUxzqEx.png "Log viewer") ![Notification screenshot](http://i.imgur.com/tebSppP.png "Notification (expanded)")
