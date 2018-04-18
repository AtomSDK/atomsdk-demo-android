# ATOM VPN SDK demo for Android  Applications
This is a demo application for Android Application with basic usage of ATOM VPN SDK which will help the developers to create smooth applications over ATOM SDK quickly.

## SDK Features covered in this Demo

* Connection with Parameters
* Connection with Pre-Shared Key (PSK)
* Connection with Dedicated IP
* Connection with Multiple Protocols (Auto-Retry Functionality)
* Connection with Real-time Optimized Servers (Countries based on latency from user in Real-time)
 
## Compatibility
 
* Compatible with Android 4.0/API Level: 14 (ICE_CREAM_SANDWICH) and later  
* Compatible with ATOM SDK Version 1.0.3 and onwards 

## Supported Protocols

* TCP
* UDP
* IKEv 
 
## SDK Installation
 
Although ATOM SDK library is already compiled with the demo application but you can install the latest version through Maven.

Add the maven url repository to your root gradle.build file:
```
allprojects {
    repositories {
        maven {
            url "https://dl.bintray.com/atomsdk/com.atom.sdk"
        }
    }
}
```
Add the dependency in app gradle, where VERSION_NAME is a version from bintray.
```
compile 'com.atom.sdk:AtomSdk:VERSION_NAME'
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
AtomConfiguration.Builder atomConfigurationBuilder = new AtomConfiguration.Builder(getString(R.string.atom_secret_key));
AtomConfiguration atomConfiguration = atomConfigurationBuilder.build();

AtomManager.initialize(this, atomConfiguration, new AtomManager.InitializeCallback() {
            @Override
            public void onInitialized() {

            }
      }
);
```
PS: ATOM SDK is a singleton, and must be initialized before accessing its methods, otherwise NullPointerException will be thrown.

## Events to Register

ATOM SDK offers five events to register for the ease of the developer.
 
* onStateChange
* onConnected
* onDisconnected
* onDialError
* onRedialing

Details of these events can be seen in the inline documentation or method summaries. You need to register these events to get notified about what’s happening behind the scenes
 
```
AtomManager.addVPNStateListener(VPNStateListener this);
```
Events will be registered with the respective callbacks for the ease of the developer.

```
 	@Override
    public void onStateChange(String state) {
       
    }
    
 	@Override
    public void onConnected() {
        
    }
    
    @Override
    public void onDisconnected(boolean isCancelled) {
        
    }
    
    @Override
    public void onDialError(String vpnErrorMessage, int vpnErrorCode, AtomException exception, ConnectionDetails connectionDetails) {
        
    }

    @Override
    public void onRedialing(String vpnErrorMessage, int vpnErrorCode, AtomException exception, ConnectionDetails connectionDetails) {
        
    }
```

## Packet Transmitted Event

ATOM SDK offers an additional event onPacketTransmitted only trigger while connected using TCP or UDP to read in/out packet transmitted.
 
``` 
	@Override
    public void onPacketsTransmitted(String in, String out) {
        
    }
```


## VPN Authentication

ATOM SDK provided two ways to authenticate your vpn user.
First one is to offer VPN Credentials directly to the SDK which you may create through the Admin Panel provided by ATOM.

```
AtomManager.getInstance().setVPNCredentials(new VPNCredentials(String VPNUsername,String VPNPassword));
```
Alternatively, if you don’t want to take hassle of creating users yourself, leave it on us and we will do the rest for you! 

```
AtomManager.getInstance().setUUID(String UniqueUserID);
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
AtomManager.getInstance().getCountries(new Callback<List<Country>>() {
            @Override
            public void onSuccess(List<Country> countries) {

            }

            @Override
            public void onError(String error, int errorCode, AtomException exception) {

            }

            @Override
            public void onNetworkError(String error, AtomException exception) {

            }
        });
```

## Fetch Protocols

Protocols can be obtained through ATOM SDK.

```
AtomManager.getInstance().getProtocols(new CollectionCallback<Protocol>() {
 	    	@Override
            public void onSuccess(List<Protocol> protocols) {

            }

            @Override
            public void onError(String error, int errorCode, AtomException exception) {

            }

            @Override
            public void onNetworkError(String error,AtomException exception) {  

            }
        }); 
```
## How to Connect

As soon as you call Connect method, the events you were listening to will get the updates about the states being changed and Dial Error (if any occurs) as well.

After initializing the VPNProperties, just call Connect method of ATOM SDK.
 
### Connection with Parameters

It is the simplest way of connection which is well explained in the steps above. You just need to provide the Country and the Protocol objects and call the Connect method.

```
VPNProperties.Builder vpnPropertiesBuilder = new VPNProperties.Builder(Country country, Protocol protocol);
VPNProperties vpnProperties = vpnPropertiesBuilder.build(); 

AtomManager.getInstance().connect(this, vpnProperties);
```

### Connection with Pre-Shared Key (PSK)

In this way of connection, it is pre-assumed that you have your own backend server which communicates with ATOM Backend APIs directly and creates a Pre-Shared Key (usually called as PSK) which you can then provide to the SDK for dialing. While providing PSK, no VPN Property other than PSK is required to make the connection. ATOM SDK will handle the rest.
```
VPNProperties vpnProperties = new VPNProperties.Builder(String PSK).build();

AtomManager.getInstance().connect(this, vpnProperties);
```
 
### Connection with Dedicated IP

You can also make your user comfortable with this type of connection by just providing them with a Dedicated DNS Host and they will always connect to a dedicated server! For this purpose, ATOM SDK provides you with the following constructor.
```
VPNProperties vpnProperties = new VPNProperties.Builder(String dedicatedHostName, Protocol protocol, String ServerType).build();

AtomManager.getInstance().connect(this, vpnProperties);
```

### Connection with Real-time Optimized Servers

This one is same as the first one i.e. “Connection with Parameters” with a slight addition of using Real-time optimized servers best from your user’s location. You just need to call "withOptimization" and rest will be handled by the ATOM SDK.
```
VPNProperties.Builder vpnPropertiesBuilder = new VPNProperties.Builder(Country country, Protocol protocol).withOptimization();
VPNProperties vpnProperties = vpnPropertiesBuilder.build(); 

AtomManager.getInstance().connect(this, vpnProperties);

```
If you want to show your user the best location for him on your GUI then ATOM SDK have it ready for you as well! ATOM SDK has a method exposed namely “getOptimizedCountries” which has a method “getLatency()” in the country object which has the real-time latency of all countries from your user’s location (only if ping is enabled on your user’s system and ISP doesn’t blocks any of our datacenters). You can use this property to find the best speed countries from your user’s location.

 
### Connection with Multiple Protocols (Auto-Retry Functionality)

You can provide THREE Protocols at max so ATOM SDK can attempt automatically on your behalf to get your user connected with the Secondary or Tertiary Protocol if your base Protocol fails to connect. 

```
VPNProperties.Builder vpnPropertiesBuilder = new VPNProperties.Builder(Country country, Protocol protocol);
vpnPropertiesBuilder.withSecondaryProtocol(Protocol secondaryProtocol);
vpnPropertiesBuilder.withTertiaryProtocol(Protocol tertiaryProtocol);
VPNProperties vpnProperties = vpnPropertiesBuilder.build(); 

AtomManager.getInstance().connect(this, vpnProperties);
```
 
For more information, please see the inline documentation of VPNProperties Class.


# Cancel Connection

You can Cancel connection between dialing process by calling following method.
```
AtomManager.getInstance().cancel(Context context);
```
# VPN Disconnection

 To disconnect, simply call the Disconnect method of AtomManager.

```
AtomManager.getInstance().diconnect(Context context);
```
 
