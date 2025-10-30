# ATOM VPN SDK demo for Android  Applications
This is a demo application for Android Application with basic usage of ATOM VPN SDK which will help the developers to create smooth applications over ATOM SDK quickly.

## SDK Features covered in this Demo
* Connection with Parameters
* Connection with Dedicated IP
* Connection with Multiple Protocols (Auto-Retry Functionality)
* Connection with Real-time Optimized Servers (Countries based on latency from user in Real-time)
* Connection with Smart Dialing (Use getCountriesForSmartDialing() to get the Advanced VPN Dialing supported countries)

## Compatibility

* Compatible with Android 7.0/API Level: 24 (Nougat) and later (Supports Android API Level 36)
* Compatible with ATOM SDK Version 5.0.0 and onwards
* Compatible with Java 17 & Kotlin 2.0.0

## Recommended

* Java 17 or higher
* Gradle 8.14.1 or higher
* AGP 8.11.1 or higher
* Compile SDK 35 or higher
* Kotlin 2.0.0 or higher

## Supported Protocols
* TCP
* UDP
* IKEV
* WIREGUARD

## SDK Installation
To use this library you should add **jitpack** repository.

Add **authToken=jp_l1hv3212tltdau845qago2l4e** in gradle.properties of your root project

Add this to root **build.gradle**

    allprojects {
        repositories {
            mavenCentral()
            maven { url 'https://jitpack.io'
                credentials { username authToken }
            }
            
            maven { url "https://bitbucket.org/purevpn/atom-android-releases/raw/master" }
        }
    }

And then add dependencies in build.gradle of your app module.
```groovy
dependencies {
    implementation 'org.bitbucket.purevpn:purevpn-sdk-android:7.0.1'
}
```
>To successfully build ATOM SDK, developer must migrate their project to AndroidX. Developer can use **Refactor** -> **Migrate to AndroidX** option in Android Studio.

>Developer must enable Kotlin support in Android Studio using Kotlin Extension.

If your application supports a minimum SDK version of 23 or higher, you **must** include one of the following configurations:

Add the following property in your application's `AndroidManifest.xml` file:
```
<application
    ...
    android:extractNativeLibs="true" 
    ...
    >
```

**OR**

Alternatively, add this configuration to your app's `build.gradle` (Groovy) file:
```
android {
    ...
    
    packagingOptions {
       jniLibs {
            useLegacyPackaging true
       }
    }
    
}
```

**OR**

If you are using Kotlin DSL, add the following configuration to your app's `build.gradle.kts` file:

```
android {
    ...
    
    packaging {
        jniLibs { 
            useLegacyPackaging = true
        }
    }
}
```


### Setup Kotlin Extension in Android Studio

Add Kotlin gradle plugin to project build.gradle
```
classpath "org.jetbrains.kotlin:kotlin-gradle-plugin:2.0.0"
```

Add Kotlin Android Extension plugin to app build.gradle
```
apply plugin: 'kotlin-android'
apply plugin: 'kotlin-parcelize'
```
Add Kotlin support to app build.gradle in dependencies
```
implementation "org.jetbrains.kotlin:kotlin-stdlib-jdk8:2.0.0"
```

# Getting Started with the Code
ATOM SDK needs to be initialized with a “SecretKey” provided to you after you buy the subscription which is typically a hex-numeric literal.

Don’t forget to change the following entry with your SECRET KEY.

```
// Put Atom SDK Secret Key here
<string name="atom_secret_key"></string>
```
ATOM SDK should be initialize in Application's onCreate method.

```
// Configure ATOM Notification
AtomNotification.Builder atomNotificationBuilder = new AtomNotification.Builder(NOTIFICATION_ID,"Atom SDK Demo","You are now secured with Atom",R.drawable.ic_stat_icn_connected, Color.BLUE);

// Configure ATOM SDK
AtomConfiguration.Builder atomConfigurationBuilder = new AtomConfiguration.Builder(R.string.atom_secret_key);
atomConfigurationBuilder.setVpnInterfaceName("Atom SDK Demo");
atomConfigurationBuilder.setNotification(atomNotificationBuilder.build());

AtomConfiguration atomConfiguration = atomConfigurationBuilder.build();

AtomManager.initialize(this, atomConfiguration, new AtomManager.InitializeCallback() {
            @Override
            public void onInitialized(AtomManager atomManager) {
            // Get initialized AtomManager instance here
       }
});
```
```PS:``` ATOM SDK is a singleton, and must be initialized before accessing its methods, otherwise NullPointerException will be thrown.

## Enable Local Inventory Support
ATOM SDK offers a feature to enable the local inventory support. This can help Application to fetch Countries and Protocols even when device network is not working.

* To enable it, Log In to the Atom Console
* Download the local data file in json format
* File name should be localdata.json. Please rename the file to localdata.json if you find any discrepancy in the file name.
* Paste the file in assets folder of your application.

## Callbacks to Register

ATOM SDK offers few callbacks to register for the ease of the developer.

* onStateChange
* onConnecting
* onConnected
* onPaused
* onDisconnected
* onDisconnecting
* onRedialing
* onDialError
* onUnableToAccessInternet

Details of these callbacks can be seen in the inline documentation or method summaries. You need to register these callback to get notified about what’s happening behind the scenes

```
AtomManager.addVPNStateListener(VPNStateListener this);

//For IKEV protocol bind IKEV Service
atomManager.bindIKEVStateService(activity);
```
Remove the callback using
```
AtomManager.removeVPNStateListener(VPNStateListener this);

//For IKEV protocol unbind it
atomManager.unBindIKEVStateService(activity);
```

Callbacks will be registered for the ease of the developer.

```
    @Override
    public void onStateChange(String state) {

    }
    
     @Override
    public void onConnecting(VPNProperties vpnProperties, AtomConfiguration atomConfiguration) {

    }

    @Override
    public void onConnected(ConnectionDetails connectionDetails) {

    }

    @Override
    public void onPaused(AtomException exception, ConnectionDetails connectionDetails) {
                
    }

    @Override
    public void onDisconnected(ConnectionDetails connectionDetails) {

    }

    @Override
    public void onRedialing(AtomException exception, ConnectionDetails connectionDetails) {

    }
    
    @Override
    public void onDialError(AtomException exception, ConnectionDetails connectionDetails) {

    }

    @Override
    public void onUnableToAccessInternet(AtomException atomException, ConnectionDetails connectionDetails) {

    }
    
     @Override
    public void onDisconnecting(ConnectionDetails connectionDetails) {

    }
```

## Packet Transmitted Callback

ATOM SDK offers an additional callback onPacketTransmitted only trigger while connected using TCP or UDP to read in/out packet transmitted.

```
    @Override
    public void onPacketsTransmitted(String in, String out, String inSpeed, String outSpeed) {

    }
```

## VPN Authentication

ATOM SDK provides VPN Credentials to authenticate your vpn user which you may create through the Admin Panel provided by ATOM.

```
atomManager.setVPNCredentials(new VPNCredentials(String VPNUsername,String VPNPassword));
```

# VPN Connection
You need to declare an object of “VPNProperties” Class to define your connection preferences. Details of all the available properties can be seen in the inline documentation of “VPNProperties” Class. For the least, you need to give Country and Protocol with which you want to connect.

```
VPNProperties.Builder vpnPropertiesBuilder = new VPNProperties.Builder(Country country, Protocol protocol);
VPNProperties vpnProperties = vpnPropertiesBuilder.build();
```

## Fetch Countries

You can get the Countries list through ATOM SDK.

```
atomManager.getCountries(new CollectionCallback<Country>() {

            @Override
            public void onSuccess(List<Country> countries) {

            }

            @Override
            public void onError(AtomException exception) {

            }

            @Override
            public void onNetworkError(AtomException exception) {

            }
        },DialingType.VPN);
```
## Fetch Recommended Location
You can get the Recommended Location for user's location through ATOM SDK.

```
atomManager.getRecommendedLocation(new Callback<Location>() {
                @Override
                public void onSuccess(Location recommendedLocation) {
                    
                }

                @Override
                public void onError(AtomException exception) {

                }

                @Override
                public void onNetworkError(AtomException exception) {

                }
            });
```

## Fetch Countries For Smart Dialing

You can get the Countries those support Smart Dialing through ATOM SDK.

```
atomManager.getCountriesForSmartDialing(new CollectionCallback<Country>() {

            @Override
            public void onSuccess(List<Country> countries) {

            }

            @Override
            public void onError(AtomException exception) {

            }

            @Override
            public void onNetworkError(AtomException exception) {

            }
        });
```

## Fetch Protocols

Protocols can be obtained through ATOM SDK.

```
atomManager.getProtocols(new CollectionCallback<Protocol>() {

            @Override
            public void onSuccess(List<Protocol> protocols) {

            }

            @Override
            public void onError(AtomException exception) {

            }

            @Override
            public void onNetworkError(AtomException exception) {

            }
        });
```
## Protocol switch

You can enable or disable protocol switch from VPNProperties class. By default its value is set to true.
```
    vpnPropertiesBuilder.enableProtocolSwitch(false);
```
or
```
    vpnPropertiesBuilder.enableProtocolSwitch(true);
```
## Recommanded protocol
If you didn't specify the protocol in case of Country, City and Channel dailing then Atom SDK dialed with recommanded protocol according to the specified country, city and channel. It will not work for dedicated IP.

## Use Failover
Failover is a mechanism in which Atom dialed with nearest server if requested server is busy or not found for any reason. You can control this mechanism from VPNPorperties class. By default its value is set to true.

```
    vpnPropertiesBuilder.withUseFailoverEnabled(false);
```
or
```
    vpnPropertiesBuilder.withUseFailoverEnabled(true);
```
## How to Connect

As soon as you call Connect method, the callbacks you were listening to will get the updates about the states being changed and Dial Error (if any occurs) as well.

After initializing the VPNProperties, just call Connect method of ATOM SDK.

### Connection with Parameters

It is the simplest way of connection which is well explained in the steps above. You just need to provide the Country and the Protocol objects and call the Connect method.

```
VPNProperties.Builder vpnPropertiesBuilder = new VPNProperties.Builder(Country country, Protocol protocol);
VPNProperties vpnProperties = vpnPropertiesBuilder.build();

atomManager.connect(this, vpnProperties);
```

From version 3.0.0 onwards, Atom has introduced connection with Cities and Channels. You can found their corresponding VPNProperties constructors in the Demo Application.

### Include or Exclude Server with Nas Identifier
When connecting with parameters, a server can be included or excluded with its Nas Identifier
```
List<ServerFilter> serverFilterList = new ArrayList<>();

ServerFilter serverFilterInclude = new ServerFilter("nas-identifier-here", ServerFilterType.INCLUDE);
ServerFilter serverFilterExclude = new ServerFilter("nas-identifier-here",, ServerFilterType.EXCLUDE);

serverFilterList.add(serverFilterInclude);
serverFilterList.add(serverFilterExclude);

vpnPropertiesBuilder.withServerFilter(serverFilterList);

atomManager.connect(this, vpnProperties);

```

### Connection with Dedicated IP
You can also make your user comfortable with this type of connection by just providing them with a Dedicated DNS Host and they will always connect to a dedicated server! For this purpose, ATOM SDK provides you with the following constructor.
```
VPNProperties vpnProperties = new VPNProperties.Builder(String dedicatedHostName, Protocol protocol).build();

atomManager.connect(this, vpnProperties);
```

### Connection with Real-time Optimized Servers
This one is same as the first one i.e. “Connection with Parameters” with a slight addition of using Real-time optimized servers best from your user’s location. You just need to call "withOptimization" and rest will be handled by the ATOM SDK.
```
VPNProperties.Builder vpnPropertiesBuilder = new VPNProperties.Builder(Country country, Protocol protocol).withOptimization();
VPNProperties vpnProperties = vpnPropertiesBuilder.build();

atomManager.connect(this, vpnProperties);
```
If you want to show your user the best location for him on your GUI then ATOM SDK have it ready for you as well! ATOM SDK has a method exposed namely “getOptimizedCountries” which has a method “getLatency()” in the country object which has the real-time latency of all countries from your user’s location (only if ping is enabled on your user’s system and ISP doesn’t blocks any of our datacenters). You can use this property to find the best speed countries from your user’s location.

### Connection with Smart Dialing
“Connection with Parameters” with a slight addition of using smart dialing to connect. You just need to call "withSmartDialing" and rest will handled by the ATOM SDK.
```
VPNProperties.Builder vpnPropertiesBuilder = new VPNProperties.Builder(Country country, Protocol protocol).withSmartDialing();
VPNProperties vpnProperties = vpnPropertiesBuilder.build();

atomManager.connect(this, vpnProperties);
```

### Connection with Multiple Protocols (Auto-Retry Functionality)
You can provide THREE Protocols at max so ATOM SDK can attempt automatically on your behalf to get your user connected with the Secondary or Tertiary Protocol if your base Protocol fails to connect.

```
VPNProperties.Builder vpnPropertiesBuilder = new VPNProperties.Builder(Country country, Protocol protocol);
vpnPropertiesBuilder.withSecondaryProtocol(Protocol secondaryProtocol);
vpnPropertiesBuilder.withTertiaryProtocol(Protocol tertiaryProtocol);
VPNProperties vpnProperties = vpnPropertiesBuilder.build();

atomManager.connect(this, vpnProperties);
```

For more information, please see the inline documentation of VPNProperties Class.
# Cancel VPN Connection
You can Cancel connection between dialing process by calling following method.
```
atomManager.cancel(Context context);
```
# Disconnect VPN Connection
To disconnect, simply call the Disconnect method of AtomManager.
```
atomManager.disconnect(Context context);
```
# Pause / Resume VPN Connection
This section provides details about the VPN Pause and Resume feature in the Atom SDK, allowing users to temporarily pause VPN connections either manually or for a specified duration. This feature is useful when users need to suspend VPN activity without fully disconnecting.

### Feature Overview
The VPN Pause feature enables pausing a VPN connection under specific conditions and supports two modes:
- **Manual Pause:** Pauses the VPN connection indefinitely until it is manually resumed by the user.
- **Timed Pause:** Pauses the VPN connection for a predefined duration, automatically resuming once the timer expires. Users also have the option to manually resume the connection before the timer completes.

#### Key Rules and Conditions:
1. VPN can be paused only when it is in Connected state.
2. VPN can be resumed only when it is in Paused state.
3. A paused VPN can still be disconnected using the SDK's Disconnect method.
4. During a timed pause, users can manually resume the VPN at any time using the Resume method.

### Integration Guide:

#### Enabling the Pause VPN Feature:
To enable the Pause VPN functionality, call the `enableVPNPause()` method within the AtomConfiguration, as shown below:
```
atomConfigurationBuilder.enableVPNPause()
```
By default, Pause VPN functionality is **diabled**.

#### Pause VPN delegation:
After enabling the Pause VPN functionality, you can observe pause events through the delegation method in the `VPNStateListener`, as shown below:
```
@Override
public void onPaused(AtomException exception, ConnectionDetails connectionDetails) {
                
}

```
The `onPaused` delegate provides two arguments:
- **ConnectionDetails:** Contains information related to the current VPN connection.
- **AtomException:** If an error occurs while pausing the VPN, this will contain the corresponding error code and message; otherwise, it will be null.

#### Pause / Resume VPN Connection
To pause the VPN connection for a specified interval, use the following method:
```
atomManager.pause(@NonNull PauseVPNTimer)
```
To resume the VPN connection, invoke:
```
atomManager.resume()
```
The `resume()` method will resume the VPN connection if it is currently in the paused state, On a successful resume, the onConnected delegate will be triggered. If an error occurs during the resume process, the onDialError delegate will be invoked.
```
@Override
public void onDialError(AtomException atomException, ConnectionDetails connectionDetails) {
                
}

@Override
public void onConnected(ConnectionDetails connectionDetails) {

}
```
The available options for `PauseVPNTimer` include both manual and various predefined timed durations (e.g., 5, 10, 15, 30, and 60 minutes), as defined in the enum shown below.
```
- MANUAL 
- MINUTES_5 
- MINUTES_10 
- MINUTES_15 
- MINUTES_30 
- MINUTES_60
```
- When the Pause VPN feature is enabled, the notification will display the following action buttons:
    - `Disconnect` and `Pause for 5 minutes` (when the VPN is in the connected state), or
    - `Resume` (when the VPN is in the paused state).
- When the Pause VPN feature is disabled, the notification will display only the `Disconnect` action button.
#### VPN State Management:
The SDK provides additional VPN statuses

- **PAUSING**
- **PAUSED**

You can request the current VPN state using the following method:

```
atomManager.getCurrentVpnStatus(context)
```

#### Handle State Changes:
SDK also provides all VPN states via `AtomManager.addVPNStateListener()` as below
```
@Override
public void onStateChange(String state) {
 
}
```

#### Pause/Resume Informations in Connection Details:
The following methods are available in the connection details related to this feature:
- `getPauseVPNTimer()`: Returns a `PauseVPNTimer`, indicating the duration for which the VPN is paused.
- `isVPNAutoResume()`: Returns a boolean indicating whether the VPN was resumed manually or automatically after the pause timer expired.

#### Error Handling:
Following are the error details for this feature:

| Error Code | Error Message | Description |
| :----------: | ------------- | ----------- |
| 5194 | Unable to resume, the VPN connection is not in a paused state. | If VPN is not PAUSED and try to resume vpn, the error occurs and is notified via onDialError delegates. |
| 5195 | Unable to pause, the VPN connection is not in a connected state. |If VPN is NOT connected and try to pause vpn, the error occurs and is notified via onPaused delegates. |
| 5196 | Unable to pause while Always-On is active. | If Always-On is active and try to pause vpn, the error occurs and is notified via onPaused delegates. |
| 5198 | Please enable Pause VPN feature via AtomConfiguration. | When try to pause/resume VPN without enabling the feature. |

#### Conclusion
The VPN Pause feature in the Atom SDK offers flexible control over VPN connections, allowing for both manual and timed pausing. By following the integration steps outlined, you can implement this feature in your application and efficiently manage VPN state transitions.

---

# Tracker / Ad Blocker
This section provides details about the Tracker and Ad Blocker feature in the Atom VPN SDK. This feature enables VPN applications built with the Atom SDK to block tracking scripts and advertisements, enhancing both privacy and performance.

### About This Feature
As a VPN service provider, we offer a robust SDK that allows our clients to build custom VPN applications. In our latest release, we’ve introduced support for Tracker and Ad Blocker functionality. When enabled, this feature will actively block trackers and advertisements during a VPN session. It is supported across all connection types provided by the SDK:

1. Connect with Param
2. Connect with Dedicated IP
3. Connect with Multiple Dedicated IPs
4. Connect with Dedicated VPS

### Integration Guide:

#### Enabling Tracker / Ad Blocker
To enable the Tracker or Ad Blocker while the VPN is connected, you can configure the feature through `VPNProperties` as shown below:
```
vpnPropertiesBuilder.withAtomShield(@NonNull List<AtomShieldFeature>)
```
The supported options for `AtomShieldFeature` are:
```
AtomShieldFeature.TRACKER
AtomShieldFeature.AD_BLOCKER
```
#### Observe Connection Status and Data
To monitor status and data updates, add a listener as follows:
```
atomManager.addAtomShieldListener(AtomShieldListener)
```
To remove the listener, use the following:
```
atomManager.removeAtomShieldListener(AtomShieldListener)
```
The `AtomShieldListener` interface includes two methods for observing Tracker / Ad Blocker connection status and stats:

- **Status Updates:** `onAtomShieldStatusChange(AtomShieldStatus)`
- **Data Updates:** `onAtomShieldDataReceived(@Nullable AtomShieldData)`

**Status Updates (AtomShieldStatus)**
| Status | Status Params | Description |
| ------ | ------------- |----------- |
| **Establishing(String)** | [String] Provide the Tracker/Ad Blocker establishing message. | Connecting the Tracker/Ad Blocker. |
| **Established(String)** | [String] Provide the Tracker/Ad Blocker established message. | Tracker/Ad Blocker successfully connected. |
| **Disconnected(String)** | [String] Provide the Tracker/Ad Blocker disconnected message. | Tracker/Ad Blocker disconnected. |
| **Error(AtomException)** | [AtomException] Provide the Tracker/Ad Blocker exception information. | An error occurred. See error codes below for details. |

#### Error Handling:
Following are the error details for this feature:

| Error Code | Error Message | Description |
| :----------: | ------------- | ----------- |
| 5177 | AtomShield can not be null or empty | When try to use tracker/ad blocker service and provide null OR empty in argument in VPNProperties. |
| 5178 | AtomShield is not available on PROXY protocol | When attempts to activate the tracker/ad blocker while utilizing the proxy protocol. |
| 5179 | Connection type does not support AtomShield | When the VPN connection other than Params, Dedicated Server and Dedicated IP. |
| 5180 | Unable to establish AtomShield connection | When the specified retry count has been attempted to the tracker blocker socket connection. |
| 5181 | Unable to make request to AtomShield server | When sending request to socket server but socket connection lost/not established OR socket connection closed OR When unexpectedly fails the request Or When VPN disconnected gracefully. |
| 5182 | Unable to enable AtomShield connection | When request to enable tracker/ad blocker service returns failure from server. |
| 5183 | Unable to get AtomShield stats | When requested to get the stats of tracker/ad blocker service from the server. |
| 5190 | Connection to AtomShield server disrupted.  | When the socket connection is disrupted due to internet availability or any other reason and a request cannot be made, retry the connection until the maximum retry count is reached. If the socket still fails to connect, throw the error |

**Data Updates (AtomShieldData)**

The `AtomShieldData` data class (contains a variable `counter`) that uses to track the number of blocked trackers or ads. It provides a cumulative count for both Trackers / Ads.
```
counter: Int // Number of trackers/ads blocked
```
#### Tracker / Ad Blocker Informations in Connection Details:
The following methods are available in the connection details related to this feature:
- `isTrackerBlockerRequested()`: Returns a boolean indicating whether the Tracker Blocker is requested.
- `isAdBlockerRequested()`: Returns a boolean indicating whether the Ad Blocker is requested.

**NOTE:** The Tracker/Ad blocker connection will be established upon successful VPN Connection.

#### Conclusion:
The Tracker and Ad Blocker feature in the Atom SDK allows clients to offer users enhanced privacy and an improved browsing experience. This feature seamlessly integrates with all supported VPN connection types, ensuring consistent functionality across various configurations.

---

# LAN Access Feature
Our VPN SDK now includes a new feature that enables users to access their locally connected devices over the internet while maintaining an active VPN connection. This functionality ensures seamless connectivity to local resources without compromising security.

### How it works:
By default, VPN connections restrict access to locally connected devices. However, our SDK introduces the `allowLocalNetworkTraffic()` method within the `VPNProperties` class, allowing users to toggle this capability on or off. You can enable this feature as shown below:
```
vpnPropertiesBuilder.allowLocalNetworkTraffic()
```
Our `VPNProperties` class offers a method `isAllowedLocalNetworkTraffic()` indicates whether the feature is requested.

## Proguard rules:
```
-dontwarn com.atom.sdk.**
-keep class com.atom.sdk.** { *; }
-keep interface com.atom.sdk.** { *; }

-dontwarn com.atom.core.**
-keep class com.atom.core.models.** { *; }
-keep interface com.atom.core.** { *; }

-dontwarn com.atom.proxy.**
-keep class com.atom.proxy.** { *; }
-keep interface com.atom.proxy.** { *; }
-keep interface com.purevpn.proxy.core.** { *; }

-keep class com.atom.sdk.android.** { *; }

-keep class com.pingchecker.** { *; }

-keep class de.blinkt.openvpn.** { *; }
-keep class org.spongycastle.util.** { *; }
-keep class org.strongswan.android.** { *; }

-keep class org.codehaus.jettison.** { *; }
-keep class com.thoughtworks.xstream.** { *; }
-keep class com.thoughtworks.xstream.converters.** { *; }

-keep class com.thoughtworks.xstream.annotations.** { *; }
-keep class com.thoughtworks.xstream.*
-keep class com.thoughtworks.xstream.* {
    public protected <methods>;
    public protected <fields>;
}

-dontwarn org.jetbrains.annotations.**
-keep class com.jakewharton.timber.** { *; }

#Crash cause: /lib/x86/libgojni.so (Java_lantern_Lantern__1init+190)
#Resolution: adding below rules
-keep class org.lantern.mobilesdk.**
-keep class lantern.** {*;}
-keep class go.** {*;}
```

# Resolve dependencies conflicts if any :
In case any dependency conflict is faced while building ATOM SDK with your application e.g. “Duplicate jar entry”, exclude that dependency from app build.gradle configuration. See SDK Demo Application for reference.
```
android{
    configurations {
        all*.exclude module: 'DEPENDENCY_MODULE_NAME_HERE'
    }
}
```

# Resolve issues when building an Android App Bundle:
When building an Android App Bundle, APKs generated from that app bundle that target Android 6.0 (API level 23) or higher now include uncompressed versions of your native libraries by default. This optimization avoids the need for the device to make a copy of the library and thus reduces the on-disk size of your app. If you'd rather disable this optimization, add the following to your gradle.properties file:

```
android.bundle.enableUncompressedNativeLibs = false
```

**NOTE:** `android.bundle.enableUncompressedNativeLibs` has been removed from **AGP 8.1** and its default value marked as `true`
