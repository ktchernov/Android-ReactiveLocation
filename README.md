ReactiveLocation library for Android
====================================

A trimmed back fork of the [Android Reactive Locations](https://github.com/mcharmas/Android-ReactiveLocation)
library. The library size has been reduced by trimming the library down to just the location API -
the original library also includes Geofencing and Places API.

Small library that wraps Google Play Services API in brilliant [RxJava](https://github.com/ReactiveX/RxJava)
```Observables``` reducing boilerplate to minimum.

Current stable version - 1.1
---------------

**This version works with Google Play Services 10.2.1 and RxJava 1.2.9+**

What can you do with that?
--------------------------

* easily connect to Play Services API
* obtain last known location
* subscribe for location updates
* use location settings API

How does the API look like?
----------------------------

Simple. All you need is to create ```ReactiveLocationProvider``` using your context.
All observables are already there. Examples are worth more than 1000 words:


### Getting last known location

```java
ReactiveLocationProvider locationProvider = new ReactiveLocationProvider(context);
locationProvider.getLastKnownLocation()
    .subscribe(new Action1<Location>() {
        @Override
        public void call(Location location) {
            doSthImportantWithObtainedLocation(location);
        }
    });
```


### Subscribing for location updates

```java
LocationRequest request = LocationRequest.create() //standard GMS LocationRequest
                                  .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                                  .setNumUpdates(5)
                                  .setInterval(100);

ReactiveLocationProvider locationProvider = new ReactiveLocationProvider(context);
Subscription subscription = locationProvider.getUpdatedLocation(request)
    .filter(...)    // you can filter location updates
    .map(...)       // you can map location to sth different
    .flatMap(...)   // or event flat map
    ...             // and do everything else that is provided by RxJava
    .subscribe(new Action1<Location>() {
        @Override
        public void call(Location location) {
            doSthImportantWithObtainedLocation(location);
        }
    });
```

When you are done (for example in ```onStop()```) remember to unsubscribe.

```java
subscription.unsubscribe();
```

### Subscribing for Activity Recognition

Getting activity recognition is just as simple

```java

ReactiveLocationProvider locationProvider = new ReactiveLocationProvider(context);
Subscription subscription = locationProvider.getDetectedActivity(0) // detectionIntervalMillis
    .filter(...)    // you can filter location updates
    .map(...)       // you can map location to sth different
    .flatMap(...)   // or event flat map
    ...             // and do everything else that is provided by RxJava
    .subscribe(new Action1<ActivityRecognitionResult>() {
        @Override
        public void call(ActivityRecognitionResult detectedActivity) {
            doSthImportantWithObtainedActivity(detectedActivity);
        }
    });
```

### Creating observable from PendingResult

If you are manually using Google Play Services and you are dealing with
```PendingResult``` you can easily transform them to observables with
```ReactiveLocationProvider.fromPendingResult()``` method.

### Cooler examples

Do you need location with certain accuracy but don't want to wait for it more than 4 sec? No problem.

```java
LocationRequest req = LocationRequest.create()
                         .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                         .setExpirationDuration(TimeUnit.SECONDS.toMillis(LOCATION_TIMEOUT_IN_SECONDS))
                         .setInterval(LOCATION_UPDATE_INTERVAL);

Observable<Location> goodEnoughQuicklyOrNothingObservable = locationProvider.getUpdatedLocation(req)
            .filter(new Func1<Location, Boolean>() {
                @Override
                public Boolean call(Location location) {
                    return location.getAccuracy() < SUFFICIENT_ACCURACY;
                }
            })
            .timeout(LOCATION_TIMEOUT_IN_SECONDS, TimeUnit.SECONDS, Observable.just((Location) null), AndroidSchedulers.mainThread())
            .first()
            .observeOn(AndroidSchedulers.mainThread());

goodEnoughQuicklyOrNothingObservable.subscribe(...);
```


How to use it?
--------------

Library is available in maven central.

### Gradle

Just use it as dependency in your *build.gradle* file
along with Google Play Services and RxJava.

```groovy

allprojects {
	repositories {
		// ...
		maven { url "https://jitpack.io" }
	}
}

dependencies {
    ...
    compile 'com.github.ktchernov:Android-ReactiveLocation:v1.0'
    compile 'com.google.android.gms:play-services-location:8.4.0' //you can use newer GMS version if you need
    compile 'io.reactivex:rxjava:1.2.9' //you can override RxJava version with a newer 1.x version if you wish
}
```

Sample
------

Sample usage is available in *sample* directory.

Places API requires API Key. Before running samples you need to create project on API console
and obtain API Key using this [guide](https://developers.google.com/places/android/signup).
Obtained key should be exported as gradle property named: ```REACTIVE_LOCATION_GMS_API_KEY``` for
example in ```~/.gradle/gradle.properties```.


License
=======

    Copyright (C) 2017 Konstantin Tchernov (https://github.com/ktchernov)

    Copyright (C) 2015 Michał Charmas (http://blog.charmas.pl)

	Licensed under the Apache License, Version 2.0 (the "License");
	you may not use this file except in compliance with the License.
	You may obtain a copy of the License at

	     http://www.apache.org/licenses/LICENSE-2.0

	Unless required by applicable law or agreed to in writing, software
	distributed under the License is distributed on an "AS IS" BASIS,
	WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
	See the License for the specific language governing permissions and
	limitations under the License.
