# KISSmetrics-4-Android -- KISSmetrics For Android

KISSmetrics-4-Android is an Android Library which helps interacting with the KISSmetrics Analytics Service.
http://80steve.com/post/kissmetrics-library-for-android/

## Setup

1. Download the repo:  git clone git://github.com/80steve/KISSmetrics-4-Android.git

2. cd KISSmetrics-4-Android

3. mvn install or mvn package

## Usage

```java
KISSmetricsAPI kiss = KISSmetricsAPI.sharedAPI(<API KEY>, <Application Context>);

// Track Event
kiss.recordEvent("Activated", null);

// Track Event with Parameters
HashMap<String, String> properties = new HashMap<String, String>();
properties.put("Item", "Potion");
properties.put("Amount", "10");
kiss.recordEvent("Purchase", properties);

```

## Notes

Right now, the code has been used in some of my own production work, however it has yet to be tested thoroughly.
Feel free to patch it and add more great features. Thanks.