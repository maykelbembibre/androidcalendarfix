# androidcalendarfix
This Android application tries to fix the failure that started in Android itself with Marshmallow which makes calendar notifications not be triggered in a random way. This application is a simple reminder manager. But it has got its own alarm scheduling algorithm, so it doesn't address the calendar failure that Android has got.

In order for this application to work it is necessary to take a certain set of actions depending on the brand of the device. In some brands like Xiaomi there's more than one action to take, and you must make sure that you take all of them; it won't be enough to take a subset of the actions that is not all the actions.

Samsung (1 action)

Settings > Battery > Energy management > Put Alarm Fix into the whitelist, i.e. the list of applications that will never sleep or be stopped by the system.

Xiaomi (2 actions)

Settings > Battery > Battery Saver > Choose apps > Alarm Fix > No restrictions

Settings > Permissions > Autostart > Alarm Fix > Permission granted
