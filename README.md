# ATOM VPN SDK demo for Android applications
This demo application shows basic usage of the ATOM VPN SDK, to help developers build on it quickly.

## Table of Contents

- [Compatibility](#compatibility)
- [Recommendation](#recommendation)
- [SDK Installation](#sdk-installation)
    - [Setup Kotlin support in Android Studio](#setup-kotlin-support-in-android-studio)
- [SDK features covered in this demo](#sdk-features-covered-in-this-demo)
- [Supported Protocols](#supported-protocols)
- [Automatic Protocol selection by SDK](#recommended-protocol)
- [Getting started with the code](#getting-started-with-the-code)
    - [Enable Local Inventory support](#enable-local-inventory-support)
    - [VPN Authentication](#vpn-authentication)
    - [Callbacks to register](#callbacks-to-register)
    - [Persist VPN Details](#persist-vpn-details)
- [VPN Connection](#vpn-connection)
    - [Fetch Recommended Location](#fetch-recommended-location)
    - [Recommended Protocol](#recommended-protocol)
    - [Fetch Countries](#fetch-countries)
    - [Fetch Protocols](#fetch-protocols)
    - [Protocol Switch](#protocol-switch)
    - [Use Failover](#use-failover)
    - [Split Tunneling](#split-tunneling)
    - [Reverse Split Tunneling](#reverse-split-tunneling)
    - [Connection with Multiport](#connection-with-multiport)
    - [LAN (Local Area Network)](#lan-local-area-network)
    - [How to Connect](#how-to-connect)
        - [Connection with Parameters](#connection-with-parameters)
        - [Include or Exclude Server with Nas Identifier](#include-or-exclude-server-with-nas-identifier)
        - [Connection with Dedicated IP](#connection-with-dedicated-ip)
        - [Connection with Multiple Protocols (Auto-Retry Functionality)](#connection-with-multiple-protocols-auto-retry-functionality)
- [Cancel VPN Connection](#cancel-vpn-connection)
- [Disconnect VPN Connection](#disconnect-vpn-connection)
- [Tracker / Ad Blocker](#tracker--ad-blocker)
    - [Overview](#overview)
    - [Enabling Tracker / Ad Blocker](#enabling-tracker--ad-blocker)
    - [Observe Connection Status and Data](#observe-connection-status-and-data)
    - [Troubleshooting](#troubleshooting)
    - [Tracker / Ad Blocker Information in Connection Details](#tracker--ad-blocker-information-in-connection-details)
- [Network Traffic Updates](#network-traffic-updates)
- [Pause / Resume VPN Connection](#pause--resume-vpn-connection)
    - [Key Rules and Conditions](#key-rules-and-conditions)
    - [Enabling the Pause VPN Feature](#enabling-the-pause-vpn-feature)
    - [Pause VPN delegation](#pause-vpn-delegation)
    - [Pause / Resume VPN Connection](#pause--resume-vpn-connection-1)
    - [VPN State Management](#vpn-state-management)
    - [Handle State Changes](#handle-state-changes)
    - [Pause/Resume Information in Connection Details](#pauseresume-information-in-connection-details)
    - [Error Handling](#error-handling)
- [Resolve dependencies conflicts](#resolve-dependencies-conflicts)
- [Resolve issues when building an Android App Bundle](#resolve-issues-when-building-an-android-app-bundle)
- [Proguard rules](#proguard-rules)

## Compatibility

Minimum requirements for building against the ATOM SDK:

* Android 7.0 / API level 24 (Nougat) and later
* Java 17 (the SDK ships Java 17 bytecode)
* Kotlin 2.0.0 or higher
* AndroidX — the legacy support library is not supported

## Recommendation

This demo is built and verified with the following toolchain:

* Compile SDK and Target SDK 36
* Gradle 8.14.3
* AGP 8.11.2
* Kotlin 2.1.20
* Java 17

## SDK Installation
To use this library you should add **jitpack** repository.

Add **authToken=jp_l1hv3212tltdau845qago2l4e** in `gradle.properties` of your root project

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

Add ATOM SDK dependency in `build.gradle` of your app module.
```groovy
dependencies {
    implementation 'org.bitbucket.purevpn:purevpn-sdk-android:7.2.0'
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

### Setup Kotlin support in Android Studio

Add Kotlin gradle plugin to project `build.gradle`
```
classpath "org.jetbrains.kotlin:kotlin-gradle-plugin:2.1.20"
```

Add the Kotlin Android and Parcelize plugins to app `build.gradle`
```
apply plugin: 'kotlin-android'
apply plugin: 'kotlin-parcelize'
```
Add Kotlin support to app `build.gradle` in dependencies
```
implementation "org.jetbrains.kotlin:kotlin-stdlib-jdk8:2.1.20"
```

## SDK features covered in this demo
* Connection with Parameters
* Connection with Dedicated IP
* Connection with Multiple Protocols (Auto-Retry Functionality)

## Supported Protocols
* TCP
* UDP
* IKEV
* WIREGUARD

## Getting started with the code
ATOM SDK needs to be initialized with a “SecretKey” provided to you after you buy the subscription which is typically a hex-numeric literal.

Don’t forget to change the following entry with your SECRET KEY.

```
// Put Atom SDK Secret Key here
<string name="atom_secret_key"></string>
```
ATOM SDK should be initialized in **Application's onCreate** method.

```
// Configure ATOM Notification
AtomNotification.Builder atomNotificationBuilder = new AtomNotification.Builder(NOTIFICATION_ID,"Atom SDK Demo","You are now secured with Atom",R.drawable.ic_stat_icn_connected, Color.BLUE);

// Configure ATOM SDK
AtomConfiguration.Builder atomConfigurationBuilder = new AtomConfiguration.Builder(getString(R.string.atom_secret_key));
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
`Note:` ATOM SDK is a singleton, and must be initialized before accessing its methods, otherwise `NullPointerException` will be thrown.

### Enable Local Inventory support
ATOM SDK offers a feature to enable the local inventory support. This can help application to fetch Countries and Protocols even when device network is not working.

* To enable it, Log In to the Atom Console
* Download the local data file in json format
* File name should be localdata.json. Please rename the file to localdata.json if you find any discrepancy in the file name.
* Paste the file in assets folder of your application.

### VPN Authentication

ATOM SDK provides `VPNCredentials` on AtomManager's instance to authenticate your vpn user which you may create through the Admin Panel provided by ATOM.

```
atomManager.setVPNCredentials(new VPNCredentials(vpnUsername, vpnPassword));
```

### Callbacks to register

ATOM SDK offers a few callbacks to register for the ease of developers.

* onStateChange
* onConnecting
* onConnected
* onPaused
* onDisconnecting
* onDisconnected
* onRedialing
* onDialError
* onUnableToAccessInternet
* onSessionTraffic
* onPacketsTransmitted

Details of these callbacks can be seen in the inline documentation or method summaries. In order to get the current states of the VPN connection, you can **statically** register the `VPNStateListener` on AtomManager.

```
AtomManager.addVPNStateListener(this);

// To remove callback
AtomManager.removeVPNStateListener(this);
```


Callbacks will be registered for the ease of developers.

```
    @Override
    public void onStateChange(String state) {

    }

    // This callback method is deprecated.
    @Override
    public void onConnecting() {

    }
    
     @Override
    public void onConnecting(VPNProperties vpnProperties, AtomConfiguration atomConfiguration) {

    }

    // This callback method is deprecated.
    @Override
    public void onConnected() {

    }

    @Override
    public void onConnected(ConnectionDetails connectionDetails) {

    }

    @Override
    public void onPaused(AtomException exception, ConnectionDetails connectionDetails) {
                
    }

    // This callback method is deprecated.
    @Override
    public void onDisconnected(boolean isCancelled) {

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

    @Override
    public void onSessionTraffic(long rxBytes, long txBytes, long rxBytesPerSecond, long txBytesPerSecond) {
    
    }

    @Override
    public void onPacketsTransmitted(String in, String out, String inSpeed, String outSpeed) {

    }
```

For specifically IKEV protocol, you **must** register the `bindIKEVStateService` in Activity/Fragment on **AtomManager's instance**.
```
atomManager.bindIKEVStateService(activity);

// To remove callback
atomManager.unBindIKEVStateService(activity);
```

### Persist VPN Details
By default ATOM SDK keeps the details of the current VPN session in memory only, so they are lost when the application process is killed. Enabling this option makes the SDK store the last session's `ConnectionDetails` and `VPNProperties` in the application's private storage, so they are still available after the application restarts.

Enable it on the `AtomConfiguration` at initialization:
```
atomConfigurationBuilder.persistVPNDetails(true);
```

With the option enabled, both of the following keep returning the last known session after a process restart. With it disabled, they only reflect a session established in the current process.
```
ConnectionDetails connectionDetails = atomManager.getConnectionDetails();
VPNProperties vpnProperties = atomManager.getVPNProperties();
```

## VPN Connection
You need to declare an object of **VPNProperties** class to define your connection preferences. Details of all the available properties can be seen in the inline documentation of “VPNProperties” class. For the least, you need to give Country/City/Channel which you want to connect.

```
VPNProperties.Builder vpnPropertiesBuilder = new VPNProperties.Builder(Country country, Protocol protocol);
VPNProperties vpnProperties = vpnPropertiesBuilder.build();
```

### Fetch Recommended Location
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

### Recommended Protocol
If you do not specify the protocol in case of Country, City and Channel dialing then Atom SDK dials with recommended protocol according to the specified country, city and channel.

for example:

**Country:**
```
    VPNProperties.Builder vpnPropertiesBuilder = new VPNProperties.Builder(Country country, null);
    VPNProperties vpnProperties = vpnPropertiesBuilder.build();
```
**City:**
```
    VPNProperties.Builder vpnPropertiesBuilder = new VPNProperties.Builder(City city, null);
    VPNProperties vpnProperties = vpnPropertiesBuilder.build();
```
**Channel:**
```
    VPNProperties.Builder vpnPropertiesBuilder = new VPNProperties.Builder(Channel channel, null);
    VPNProperties vpnProperties = vpnPropertiesBuilder.build();
```

`Note:` *Recommended protocol will not work for dedicated IP*.

### Fetch Countries

Countries can be obtained through ATOM SDK.

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
        }, DialingType.VPN);
```

### Fetch Protocols

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

### Protocol switch

You can enable or disable protocol switch from VPNProperties class. It is `true` by default.
```
    vpnPropertiesBuilder.enableProtocolSwitch(false);
```
or
```
    vpnPropertiesBuilder.enableProtocolSwitch(true);
```

### Use Failover
Failover is a mechanism in which Atom dials with nearest server if requested server is busy or not found for any reason. You can control this mechanism from VPNProperties class. It is `true` by default.

```
    vpnPropertiesBuilder.withUseFailoverEnabled(false);
```
or
```
    vpnPropertiesBuilder.withUseFailoverEnabled(true);
```

### Split Tunneling
Split Tunneling routes **only the applications you list** through the VPN tunnel. Every other application on the device keeps using the normal connection. Pass the application package names to `withSplitTunneling`:

```
String[] applicationPackages = { "com.example.browser", "com.example.mail" };

vpnPropertiesBuilder.withSplitTunneling(applicationPackages);
```

Supported on all protocols — TCP, UDP, IKEv2 and WireGuard.

Package names are trimmed and blank entries are discarded, so incidental whitespace is safe. If the array is `null` or empty the feature stays off and the connection is dialed normally.

To read the state back:
```
boolean requested = vpnProperties.isSplitTunnelingEnabled();
String[] packages = vpnProperties.getApplicationPackages();

boolean active = connectionDetails.isSplitTunnelingEnabled();
```

### Reverse Split Tunneling
Reverse Split Tunneling is the inverse: **the applications you list bypass the VPN tunnel** and everything else on the device is routed through it. Pass the application package names to `withReverseSplitTunneling`:

```
String[] applicationPackages = { "com.example.banking", "com.example.streaming" };

vpnPropertiesBuilder.withReverseSplitTunneling(applicationPackages);
```

Supported on all protocols — TCP, UDP, IKEv2 and WireGuard. Package names are cleaned the same way, and a `null` or empty array leaves the feature off.

To read the state back:
```
String[] packages = vpnProperties.getApplicationPackages();
boolean requested = vpnProperties.isReverseSplitTunnelingEnabled;   // public field, not a getter

boolean active = connectionDetails.isReverseSplitTunnelingEnabled();
```

`Note:` Split Tunneling and Reverse Split Tunneling are **mutually exclusive** and share a single application list. Calling one clears the other, so the last call wins — set whichever mode you want once per `VPNProperties`, not both.

### Connection with Multiport
Atom SDK dials the VPN connection with Multiport or opened port other than the protocol's default, which helps when the default port is throttled or blocked on the user's network. The port can either
be chosen by ATOM SDK or specified by the application.

Let ATOM SDK pick the port:
```
vpnPropertiesBuilder.withAutomaticPort();
```

Or dial a specific port: *(Supported port range can be retrieved from protocol's object)*
```
vpnPropertiesBuilder.withManualPort(5500); // Only pass the valid port range
```

`Note:` Multiport applies to the **OpenVPN protocols only — TCP and UDP**. It also requires the selected server to advertise multiport support. When either condition is not met, the connection is dialed on the standard port instead; this is a silent fallback and no error is reported.

`Note:` `withAutomaticPort()` and `withManualPort(int)` both enable Multiport but are **mutually exclusive** — each one clears the other, so the last call wins. Call whichever mode
you want once per `VPNProperties`, not both. A port of `0` or less passed to `withManualPort(int)` is ignored and leaves the builder untouched.

To read the state back:
```
boolean multiport = vpnProperties.isMultiPortEnabled();
boolean automatic = vpnProperties.isAutomaticPortEnabled();
int manualPort = vpnProperties.getManualPortNumber();
```
Once connected, the port actually used is reported on the connection details:
```
boolean dialedWithMultiport = connectionDetails.isDialedWithMultiport();
int dialedPort = connectionDetails.getDialedPort();
```

### LAN (Local Area Network)
Atom SDK includes a feature that enables users to access their locally connected devices over the internet while maintaining an active VPN connection. This functionality ensures seamless connectivity to local resources without compromising security. By default, VPN connections restrict access to locally connected devices. However, our SDK introduces the `allowLocalNetworkTraffic()` method within the `VPNProperties` class, allowing users to toggle this capability on/off.
```
vpnPropertiesBuilder.allowLocalNetworkTraffic()
```
`VPNProperties` offers an `isAllowedLocalNetworkTraffic()` method that indicates whether the feature is requested.

### How to Connect

As soon as you call Connect method, the callbacks you are listening to, will get the updates about the states being changed and Dial Error (if any occurs) as well.

After initializing the VPNProperties, just call Connect method of ATOM SDK.

#### Connection with Parameters

It is the simplest way of connection which is well explained in the steps above. You just need to provide the Country and the Protocol objects and call the Connect method.

```
VPNProperties.Builder vpnPropertiesBuilder = new VPNProperties.Builder(Country country, Protocol protocol);
VPNProperties vpnProperties = vpnPropertiesBuilder.build();

atomManager.connect(context, vpnProperties);
```


#### Include or Exclude Server with Nas Identifier
When connecting with parameters, a server can be included or excluded with its Nas Identifier
```
List<ServerFilter> serverFilterList = new ArrayList<>();

ServerFilter serverFilterInclude = new ServerFilter("nas-identifier-here", ServerFilterType.INCLUDE);
ServerFilter serverFilterExclude = new ServerFilter("nas-identifier-here", ServerFilterType.EXCLUDE);

serverFilterList.add(serverFilterInclude);
serverFilterList.add(serverFilterExclude);

vpnPropertiesBuilder.withServerFilter(serverFilterList);

atomManager.connect(context, vpnProperties);

```

#### Connection with Dedicated IP
You can also make your user comfortable with this type of connection by just providing them with a Dedicated DNS Host and they will always connect to a dedicated server! For this purpose, ATOM SDK provides you with the following constructor.
```
VPNProperties vpnProperties = new VPNProperties.Builder(String dedicatedHostName, Protocol protocol).build();

atomManager.connect(context, vpnProperties);
```

#### Connection with Multiple Protocols (Auto-Retry Functionality)
You can provide THREE Protocols at max so ATOM SDK can attempt automatically on your behalf to get your user connected with the Secondary or Tertiary Protocol if your base Protocol fails to connect.

```
VPNProperties.Builder vpnPropertiesBuilder = new VPNProperties.Builder(Country country, Protocol protocol);
vpnPropertiesBuilder.withSecondaryProtocol(Protocol secondaryProtocol);
vpnPropertiesBuilder.withTertiaryProtocol(Protocol tertiaryProtocol);
VPNProperties vpnProperties = vpnPropertiesBuilder.build();

atomManager.connect(context, vpnProperties);
```

For more information, please see the inline documentation of VPNProperties class.
## Cancel VPN Connection
You can cancel connection between dialing process by calling following method.
```
atomManager.cancel(context);
```
## Disconnect VPN Connection
To disconnect, simply call the disconnect method of AtomManager.
```
atomManager.disconnect(context);
```

---

## Tracker / Ad Blocker
This feature enables VPN applications built with the Atom SDK to block tracking scripts and advertisements, enhancing both **privacy** and **performance**.

### Overview
As a VPN service provider, we offer a robust SDK that allows our clients to build custom VPN applications. We have introduced support for Tracker and Ad Blocker functionality. When enabled, this will actively block trackers and advertisements during the VPN session. It is supported across all connection types provided by the SDK:

1. Connect with Params
2. Connect with Dedicated IP
3. Connect with Multiple Dedicated IPs
4. Connect with Dedicated VPS

### Enabling Tracker / Ad Blocker
To enable the Tracker and Ad Blocker while the VPN is connected, you can configure the feature through `VPNProperties` as shown below:
```
vpnPropertiesBuilder.withAtomShield(features) //features = List<AtomShieldFeature>
```
From Atom SDK version **7.1.0** onwards, the Tracker and Ad Blocker offerings are combined into a single option that maximizes privacy during the VPN session:
```
AtomShieldFeature.TRACKER_AND_AD_BLOCKER
```
**NOTE:** On Atom SDK versions **before 7.1.0**, the two were opted into separately as `AtomShieldFeature.TRACKER` and `AtomShieldFeature.AD_BLOCKER`. Both were replaced by `TRACKER_AND_AD_BLOCKER` in 7.1.0, so code still referencing them will not compile against 7.1.0 or later.

### Observe Connection Status and Data
To monitor status and data updates, add a listener as follows:
```
atomManager.addAtomShieldListener(this) // this = AtomShieldListener
```
To remove the listener, use the following:
```
atomManager.removeAtomShieldListener(this) // this = AtomShieldListener
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

### Troubleshooting:
Following are the error details for this feature:

| Error Code | Error Message | Description |
| :----------: | ------------- | ----------- |
| 5177 | AtomShield can not be null or empty | When try to use tracker/ad blocker service and provide null OR empty in argument in VPNProperties. |
| 5179 | Connection type does not support AtomShield | When the VPN connection other than Params, Dedicated VPS, Dedicated IP and Multiple Dedicated IPs. |
| 5180 | Unable to establish AtomShield connection | When the specified retry count has been attempted to the tracker blocker socket connection. |
| 5181 | Unable to make request to AtomShield server | When sending request to socket server but socket connection lost/not established OR socket connection closed OR When unexpectedly fails the request Or When VPN disconnected gracefully. |
| 5182 | Unable to enable AtomShield connection | When request to enable tracker/ad blocker service returns failure from server. |
| 5183 | Unable to get AtomShield stats | When the request for the stats fails. |
| 5190 | Connection to AtomShield server disrupted.  | When the socket connection is disrupted due to internet availability or any other reason and a request cannot be made, retry the connection until the maximum retry count is reached. If the socket still fails to connect, throw the error |

**Data Updates (AtomShieldData)**

`AtomShieldData` carries a single `counter` field holding the cumulative number of blocked trackers and ads.

```
counter: Int // Number of trackers/ads blocked
```
### Tracker / Ad Blocker Information in Connection Details:
The following method is available in the connection details related to this feature:
- `isTrackerAndAdBlockerRequested()`: Returns a boolean indicating whether the Tracker and Ad Blocker was requested.

**NOTE:** On Atom SDK versions **before 7.1.0**, this was reported by two separate methods, `isTrackerBlockerRequested()` and `isAdBlockerRequested()`. Both were replaced by `isTrackerAndAdBlockerRequested()` in 7.1.0.

---

## Network Traffic Updates

ATOM SDK offers additional callbacks `onSessionTraffic` and `onPacketsTransmitted` to read in/out packet transmitted (all supported protocols).

**What is counted?** Only traffic that crossed the tunnel, on ***OpenVPN (TCP/UDP)***, ***WireGuard*** and ***IKEv2***. Applications excluded by split tunnelling are not counted, and neither is any other network activity on the device.

**Threading:** Always delivered on the main thread, on every protocol. Safe to update views directly from the callback.

**Measurement basis:** *OpenVPN* and *WireGuard* report `link-level` bytes — encrypted, including per-packet protocol overhead, handshakes and keepalives. *IKEv2* reports `payload` bytes, excluding ESP overhead. Identical user activity therefore reads a few percent lower on *IKEv2*. Neither basis is an estimate; where an exact figure matters, treat the protocol as part of the reading.

**Session boundaries:** `rxBytes` (Download) and `txBytes` (Upload) are cumulative for the current tunnel session and reset to 0 whenever a new one begins. They are otherwise non-decreasing, so a decrease reliably signals a new session. Usage across a whole connection must be summed per session rather than read from the last value.

`onSessionTraffic` — raw byte counters, available from Atom SDK version **7.2.0**. It is a **default** method, so listeners written against earlier versions compile and run unchanged after upgrading.

```
    @Override
    public void onSessionTraffic(long rxBytes, long txBytes, long rxBytesPerSecond, long txBytesPerSecond) {
    
    }
```

`onPacketsTransmitted` (available in all Atom SDK versions) reports the same measurement, *formatted* for display. Units are binary (`B`/`KB`/`MB`/`GB`/`TB` step by `1024`, not 1000) and the format is locale-independent — always with ASCII digits.

```
    @Override
    public void onPacketsTransmitted(String in, String out, String inSpeed, String outSpeed) {

    }
```

---

## Pause / Resume VPN Connection
This feature allowing users to temporarily pause VPN connections either manually or for a specified duration. Useful when users need to suspend VPN activity without fully disconnecting.

The VPN Pause feature enables pausing a VPN connection under specific conditions and supports two modes:
- **Manual Pause:** Pauses the VPN connection indefinitely until it is manually resumed by the user.
- **Timed Pause:** Pauses the VPN connection for a predefined duration, automatically resuming once the timer expires. Users also have the option to manually resume the connection before the timer completes.

### Key Rules and Conditions:
1. VPN can be paused only when it is in Connected state.
2. VPN can be resumed only when it is in Paused state.
3. A paused VPN can still be disconnected using the SDK's Disconnect method.
4. During a timed pause, users can manually resume the VPN at any time using the Resume method.

### Enabling the Pause VPN Feature:
To enable the Pause VPN functionality, call the `enableVPNPause()` method within the AtomConfiguration, as shown below:
```
atomConfigurationBuilder.enableVPNPause()
```
By default, Pause VPN functionality is **disabled**.

### Pause VPN delegation:
After enabling the Pause VPN functionality, you can observe pause events through the delegation method in the `VPNStateListener`, as shown below:
```
@Override
public void onPaused(AtomException exception, ConnectionDetails connectionDetails) {
                
}

```
The `onPaused` delegate provides two arguments:
- **AtomException:** If an error occurs while pausing the VPN, this will contain the corresponding error code and message; otherwise, it will be null.
- **ConnectionDetails:** Contains information related to the current VPN connection.

### Pause / Resume VPN Connection
To pause the VPN connection, use the following method:
```
atomManager.pause(pauseVPNTimer)
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

When the resuming requires user's will paused state VPN connection, `MANUAL` option is the choice.
```
- MANUAL 
```

Otherwise specific timer can be selected as:

```
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

### VPN State Management:
The SDK provides additional VPN statuses

- **PAUSING**
- **PAUSED**

You can request the current VPN state using the following method:

```
atomManager.getCurrentVpnStatus(context)
```

### Handle State Changes:
SDK also provides all VPN states via `AtomManager.addVPNStateListener()` as below
```
@Override
public void onStateChange(String state) {
 
}
```

### Pause/Resume Information in Connection Details:
The following methods are available in the connection details related to this feature:
- `getPauseVPNTimer()`: Returns a `PauseVPNTimer`, indicating the duration for which the VPN is paused.
- `isVPNAutoResume()`: Returns a boolean indicating whether the VPN was resumed manually or automatically after the pause timer expired.

### Error Handling:
Following are the error details for this feature:

| Error Code | Error Message | Description |
| :----------: | ------------- | ----------- |
| 5194 | Unable to resume, the VPN connection is not in a paused state. | If VPN is not PAUSED and try to resume vpn, the error occurs and is notified via onDialError delegates. |
| 5195 | Unable to pause, the VPN connection is not in a connected state. |If VPN is NOT connected and try to pause vpn, the error occurs and is notified via onPaused delegates. |
| 5196 | Unable to pause while Always-On is active. | If Always-On is active and try to pause vpn, the error occurs and is notified via onPaused delegates. |
| 5198 | Please enable Pause VPN feature via AtomConfiguration. | When try to pause/resume VPN without enabling the feature. |

---

## Resolve dependencies conflicts:
In case any dependency conflict is faced while building ATOM SDK with your application e.g. “Duplicate jar entry”, exclude that dependency from app build.gradle configuration. See SDK Demo Application for reference.
```
android{
    configurations {
        all*.exclude module: 'DEPENDENCY_MODULE_NAME_HERE'
    }
}
```

## Resolve issues when building an Android App Bundle:
When building an Android App Bundle, APKs generated from that app bundle that target Android 6.0 (API level 23) or higher now include uncompressed versions of your native libraries by default. This optimization avoids the need for the device to make a copy of the library and thus reduces the on-disk size of your app. If you'd rather disable this optimization, add the following to your gradle.properties file:

```
android.bundle.enableUncompressedNativeLibs = false
```

**NOTE:** `android.bundle.enableUncompressedNativeLibs` has been removed from **AGP 8.1** and its default value marked as `true`


## Proguard rules:
```
-dontwarn com.atom.sdk.**
-keep class com.atom.sdk.** { *; }
-keep interface com.atom.sdk.** { *; }

-dontwarn com.atom.core.**
-keep class com.atom.core.models.** { *; }
-keep interface com.atom.core.** { *; }

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

