/*
 * Copyright (c) 2018 ATOM SDK Demo.
 * All rights reserved.
 */

package com.atom.vpn.demo.common;



public class Constants {

    public static final String HostRequired = "Enter a host";
    public static final String SecretKeyRequired = "Secret Key is required.";
    public static final String DisconnectBeforeExit = "Disconnect VPN before closing app";
    public static final String PSKRequired = "Enter PSK";
    public static final String UUIDRequired = "UUID is required";
    public static final String UsernameRequired = "Username is required";
    public static final String PasswordRequired = "Password is required";

    public static final String TooltipAutoGenCred = "If enabled, a unique user identifier (UUID) is required to generate username and password.";
    public static final String TooltipUUID = "A unique user identifier such as an email, to generate username and password";
    public static final String TooltipCred = "Credentials required for connection when &quot;Auto Generate user credentials&quot; is not checked.";
    public static final String TooltipPSK = "A pre-shared key is generated using your selection of country or protocol which is used to get fastest servers for connection.";
    public static final String TooltipDedIP = "A dedicated IP/host is allowed to particular username. Enter if you are allowed one.";
    public static final String TooltipSkipVerify = "Connects to your specified host even if not allowed to your username (or when using Auto Generate user credentials)";
    public static final String TooltipPrimaryProtocol = "This protocol will be used as primary protocol to dial the vpn connection.";
    public static final String TooltipSecondaryProtocol = "This protocol will be used as secondary protocol to dial the vpn connection.";
    public static final String TooltipTertiaryProtocol = "This protocol will be used as tertiary protocol to dial the vpn connection.";
    public static final String TooltipCountry = "An attempt to connect will get fastest servers from the selected country.";
    public static final String TooltipOptimization = "If enabled, fastest servers will be fetched based on the smartest ping response.";
    public static final String TooltipCallbacks = "Displays connection callbacks";
    public static final String TooltipSmartDialing = "If enabled, ATOM SDK will use smart dialing mechanism to connect to desired country.";

}