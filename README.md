OBD-II Cloud Tracker
====================

Android service that interfaces with car via a Scantool OBDLink MX (ELM327 compatable) OBD-II reader.

Data points are gathered from the car and various sensors on the mobile phone on an interval and/or as changes occur.  These data points are bound to JSON, compressed (zlib), and sent to an attached ZeroMQ server on a set interval.  (Server not included).

###Data Points
- Location (GPS/A-GPS)
- Vehicle Speed (MPH)
- Engine Temperature (&deg;F)
- Fuel Efficiency (MPG)
- RPM

###Sample JSON
```json
{
    "command": "store",
    "payload": {
        "engine_temp": 183.2,
        "id": "live",
        "location": {
            "lat": 37.6940084,
            "lng": -122.4702936,
            "timestamp": 1426053662
        },
        "mpg": 28.513067,
        "speed": 69.59355,
        "rpm": 2845,
        "timestamp": 1426053662
    }
}
```
