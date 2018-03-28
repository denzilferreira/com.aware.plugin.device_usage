AWARE Plugin: Device Usage
==========================

[![Release](https://jitpack.io/v/denzilferreira/com.aware.plugin.device_usage.svg)](https://jitpack.io/#denzilferreira/com.aware.plugin.device_usage)

This plugin measures the device usage and non-usage sessions.

# Settings
Parameters adjustable on the dashboard and client:
- **status_plugin_device_usage**: (boolean) activate/deactivate plugin

# Broadcasts
**ACTION_AWARE_PLUGIN_DEVICE_USAGE**
Broadcast as sessions toggle between usage-not usage, with the following extras:
- **elapsed_device_off**: (double) amount of time turned off (milliseconds)
- **elapsed_device_on**: (double) amount of time turned on (milliseconds)

# Providers
##  Device Usage Data
> content://com.aware.plugin.device_usage.provider.device_usage/plugin_device_usage

Field | Type | Description
----- | ---- | -----------
_id | INTEGER | primary key auto-incremented
timestamp | REAL | unix timestamp in milliseconds of sample
device_id | TEXT | AWARE device ID
double_elapsed_device_on | REAL | amount of time the device was on (milliseconds)
double_elapsed_device_off	| REAL | amount of time the device was off (milliseconds)