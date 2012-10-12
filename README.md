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

## License

Copyright 2012 Steve Chan, http://80steve.com

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
