# ATOM VPN SDK demo for Android  Applications
This is a demo application for Android Application with basic usage of ATOM VPN SDK which will help the developers to create smooth applications over ATOM SDK quickly.

## SDK Features covered in this Demo
* Connection with Parameters
* Connection with Pre-Shared Key (PSK)
* Connection with Dedicated IP
* Connection with Multiple Protocols (Auto-Retry Functionality)
* Connection with Real-time Optimized Servers (Countries based on latency from user in Real-time)
* Connection with Smart Dialing (Use getCountriesForSmartDialing() to get the Advanced VPN Dialing supported countries)
* Connection with Smart Connect (Tags based dialing)

## Compatibility

* Compatible with Android 5.1/API Level: 22 (Lollipop) and later
* Compatible with ATOM SDK Version 4.0.0 and onwards

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
    implementation 'org.bitbucket.purevpn:purevpn-sdk-android:4.4.0'
}
```
>To successfully build ATOM SDK, developer must migrate their project to AndroidX. Developer can use **Refactor** -> **Migrate to AndroidX** option in Android Studio.

>Developer must enable Kotlin support in Android Studio using Kotlin Extension.

### Setup Kotlin Extension in Android Studio

Add Kotlin gradle plugin to project build.gradle
```
classpath "org.jetbrains.kotlin:kotlin-gradle-plugin:1.3.71"
```

Add Kotlin Android Extension plugin to app build.gradle
```
apply plugin: 'kotlin-android'
apply plugin: 'kotlin-android-extensions'
```
Add Kotlin support to app build.gradle in dependencies
```
implementation "org.jetbrains.kotlin:kotlin-stdlib-jdk7:1.3.71"
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
* onDisconnected
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
```

## Packet Transmitted Callback

ATOM SDK offers an additional callback onPacketTransmitted only trigger while connected using TCP or UDP to read in/out packet transmitted.

```
    @Override
    public void onPacketsTransmitted(String in, String out, String inSpeed, String outSpeed) {

    }
```

## VPN Authentication

ATOM SDK provided two ways to authenticate your vpn user.
First one is to offer VPN Credentials directly to the SDK which you may create through the Admin Panel provided by ATOM.

```
atomManager.setVPNCredentials(new VPNCredentials(String VPNUsername,String VPNPassword));
```
Alternatively, if you don’t want to take hassle of creating users yourself, leave it on us and we will do the rest for you!

```
atomManager.setUUID(String UniqueUserID);
```

You just need to provide a Unique User ID for your user e.g. any unique hash or even user’s email which you think remains consistent and unique for your user. ATOM SDK will generate VPN Account behind the scenes automatically and gets your user connected! Easy isn’t it?

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
If you didn't specify the protocol in case of Country, City and Channel dailing then Atom SDK dialed with recommanded protocol according to the specified country, city and channel. It did not work in PSK, Smart connect dialing and dedicated IP.

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


### Connection with Pre-Shared Key (PSK)

In this way of connection, it is pre-assumed that you have your own backend server which communicates with ATOM Backend APIs directly and creates a Pre-Shared Key (usually called as PSK) which you can then provide to the SDK for dialing. While providing PSK, no VPN Property other than PSK is required to make the connection. ATOM SDK will handle the rest.
```
VPNProperties vpnProperties = new VPNProperties.Builder(String PSK).build();

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

### Connection with Smart Connect
If you want us to connect your user with what's best for him, you can now do it using SmartConnect feature. Atom has introduced an enum list of feature a.k.a Tags you want to apply over those smart connections which can be found under com.atom.core.models.SmartConnectTag package. An example usage of SmartConnect is depicted below.
```
List<SmartConnectTag> smartConnectTags = new ArrayList<>();
smartConnectTags.add(SmartConnectTag.FILE_SHARING);
VPNProperties.Builder vpnPropertiesBuilder = new VPNProperties.Builder(primaryProtocol, smartConnectTags);
VPNProperties vpnProperties = vpnPropertiesBuilder.build();

atomManager.connect(this, vpnProperties);
```
Tags aren't mandatory and is a nullable parameter. You can only provide Protocol to connect and rest Atom will manage.

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
