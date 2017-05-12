# DataUsageNotifier
Data Usage Notifier - An Android App

Who has been sending/receiving data on your phone in the background?

Starting the service gets network activity stats since the last update, and polls every 8 seconds until stopped. Works in the background.

Notification and toasts with updated stats while service is running. Pressing the notification shows a scrolling log of stats.

(Log views are cached on stopping app and restored on start.)

![Screenshot](http://i.imgur.com/B3ZV10v.jpg "Notification (expanded)") ![Screenshot](http://i.imgur.com/yelOU0q.jpg "Log viewer")
