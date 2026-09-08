/*
 * Copyright (c) 2018 ATOM SDK Demo.
 * All rights reserved.
 */

package com.atom.vpn.demo.common

object Constants {

    const val HostRequired = "Enter a host"
    const val SecretKeyRequired = "Secret Key is required."
    const val DisconnectBeforeExit = "Disconnect VPN before closing app"
    const val PSKRequired = "Enter PSK"
    const val UUIDRequired = "UUID is required"
    const val UsernameRequired = "Username is required"
    const val PasswordRequired = "Password is required"
    const val TooltipAutoGenCred =
        "If enabled, a unique user identifier (UUID) is required to generate username and password."
    const val TooltipUUID =
        "A unique user identifier such as an email, to generate username and password"
    const val TooltipCred =
        "Credentials required for connection when &quot;Auto Generate user credentials&quot; is not checked."
    const val TooltipPSK =
        "A pre-shared key is generated using your selection of country or protocol which is used to get fastest servers for connection."
    const val TooltipDedIP =
        "A dedicated IP/host is allowed to particular username. Enter if you are allowed one."
    const val TooltipSkipVerify =
        "Connects to your specified host even if not allowed to your username (or when using Auto Generate user credentials)"
    const val TooltipPrimaryProtocol =
        "This protocol will be used as primary protocol to dial the vpn connection."
    const val TooltipSecondaryProtocol =
        "This protocol will be used as secondary protocol to dial the vpn connection."
    const val TooltipTertiaryProtocol =
        "This protocol will be used as tertiary protocol to dial the vpn connection."
    const val TooltipCountry =
        "An attempt to connect will get fastest servers from the selected country."
    const val TooltipOptimization =
        "If enabled, fastest servers will be fetched based on the smartest ping response."
    const val TooltipCallbacks = "Displays connection callbacks"
    const val TooltipSmartDialing =
        "If enabled, ATOM SDK will use smart dialing mechanism to connect to desired country."
}
