/**
 *  Hubitat driver for Todki TD-ZW101 zwave switch.
 https://github.com/tmastersmart/hubitat-code/raw/main/todki_td-ZW101.groovy
 TODKI TD-ZW101    Reset device by pressing button 10 times ,, Hold down during reboot?
 
 Device supports power memory flash LED config and brightness.
 
 WARNING this is not working it is a code in process if you make changes and get it working please note the changes..
 port to hubitat is in process. Right now i cant get basic function to work
 
V Beta test 
 

 deviceType: 3
manufacturer: 806
inClusters: 0x5E,0x55,0x98,0x9F
secureInClusters: 0x86,0x25,0x70,0x85,0x8E,0x59,0x72,0x5A,0x73,0x6C,0x7A
versionReport: 86120306010401FF00
zwNodeInfo: D3 9C 80 04 10 01 5E 55 98 9F 68 23 F1 00 86 25 70 85 5C 8E 59 72 5A 73 6C 7A
 
 
 
 
 
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 *
 *sec:86,25,70,85,8E,59,72,5A,73,6C,7A 
 *
 *TODKI Wireless Smart Power Switch Plug  driver  - TD-ZW101
 *
 
 test
 
 CommandClassReport- class:0x9F, version:1
 CommandClassReport- class:0x98, version:1
 CommandClassReport- class:0x55, version:2
 CommandClassReport- class:0x5E, version:2
 
 infoConfigurationReport- parameterNumber:1, size:1, value:0
 infoConfigurationReport- parameterNumber:8, size:1, value:0
 infoConfigurationReport- parameterNumber:10, size:1, value:0 
 infoConfigurationReport- parameterNumber:11, size:1, value:85
 infoConfigurationReport- parameterNumber:12, size:1, value:2 
 

Factory provided data 
Parameter 8 default 0
0= led on when off
1= led on when on
2= led off
3= led on

Parameter 10 default 0
0= strobe off
54 = alarm is on flashes at 400ms

Paramter 11 default 85
1- 100 Led brightness

Parameter#12 default 2
0= off when power restored
1= on
2= status before falure
 


*/

metadata {
	definition(name: "TODKI TD-ZW101", namespace: "tmastersmart", author: "Tmaster", importUrl: "https://github.com/tmastersmart/hubitat-code/raw/main/todki_td-ZW101.groovy") {

	        capability "Switch"
                capability "Indicator"
		capability "Refresh"
		capability "Polling"
		capability "Actuator"
		capability "Sensor"
		capability "Health Check"
                capability "Configuration"

		fingerprint profileId: "C216", inClusters: "0x5E,0x55,0x98,0x9F", outClusters: "", manufacturer: "TODKI", model: "TD-ZW101", deviceJoinName: "TODKI TD-ZW101 Switch"

	}


	preferences
	{
		section {
			input "ledIndicator", "enum", title: "LED Indicator", description: "Turn LED indicator... ", required: false, options:["on": "When On", "off": "When Off", "never": "Never"], defaultValue: "on"
	}
		section {
			input title: "Device Debug", description: "This feature allows you log message handling to the device for troubleshooting purposes.", displayDuringSetup: false, type: "paragraph", element: "paragraph"
			input title: "Enable debug messages?", displayDuringSetup: false, type: "bool", name: "debugEnabled"
		}
	}



	// tile definitions


	tiles {
		standardTile("switch", "device.switch", width: 4, height: 4, canChangeIcon: true) {
			state "on", label: '${name}', action: "switch.off", icon: "st.switches.switch.on", backgroundColor: "#00a0dc"
			state "off", label: '${name}', action: "switch.on", icon: "st.switches.switch.off", backgroundColor: "#ffffff"
		}
		standardTile("refresh", "device.switch", inactiveLabel: false, decoration: "flat") {
			state "default", label: '', action: "refresh.refresh", icon: "st.secondary.refresh"
		}

		multiAttributeTile(name:"switch", type: "lighting", width: 6, height: 4, canChangeIcon: true){
		tileAttribute ("device.switch", key: "PRIMARY_CONTROL") {
			attributeState "on", label: '${name}', action: "switch.off", icon: "st.switches.switch.on", backgroundColor: "#00A0DC"
			attributeState "off", label: '${name}', action: "switch.on", icon: "st.switches.switch.off", backgroundColor: "#ffffff"
			}
           }
         main "switch"
		details(["switch", "refresh"])  
	}
}

def installed() {
	// Device-Watch simply pings if no device events received for checkInterval duration of 32min = 2 * 15min + 2min lag time
	sendEvent(name: "checkInterval", value: 2 * 15 * 60 + 2 * 60, displayed: false, data: [protocol: "zwave", hubHardwareId: device.hub.hardwareID, offlinePingable: "1"])
}

def updated() {
	response(refresh())
}

def parse(description) {
	def result = null
	if (description.startsWith("Err 106")) {
		result = createEvent(descriptionText: description, isStateChange: true)
	} else if (description != "updated") {
		def cmd = zwave.parse(description, [0x20: 1, 0x25: 1, 0x70: 1, 0x98: 1])
		if (cmd) {
			result = zwaveEvent(cmd)
			log.debug("'$description' parsed to $result")
		} else {
			log.debug("Couldn't zwave.parse '$description'")
		}
	}
	result
}

def zwaveEvent(hubitat.zwave.commands.basicv1.BasicReport cmd) {
	createEvent(name: "switch", value: cmd.value ? "on" : "off")
}

def zwaveEvent(hubitat.zwave.commands.basicv1.BasicSet cmd) {
	createEvent(name: "switch", value: cmd.value ? "on" : "off")
}

def zwaveEvent(hubitat.zwave.commands.switchbinaryv1.SwitchBinaryReport cmd) {
	createEvent(name: "switch", value: cmd.value ? "on" : "off")
}

def zwaveEvent(hubitat.zwave.commands.hailv1.Hail cmd) {
	createEvent(name: "hail", value: "hail", descriptionText: "Switch button was pressed", displayed: false)
}

def zwaveEvent(hubitat.zwave.commands.securityv1.SecurityMessageEncapsulation cmd) {
	def encapsulatedCommand = cmd.encapsulatedCommand([0x20: 1, 0x25: 1])
	if (encapsulatedCommand) {
		zwaveEvent(encapsulatedCommand)
	}
}

def zwaveEvent(hubitat.zwave.Command cmd) {
	log.debug "Unhandled: $cmd"
	null
}

def zwaveEvent(hubitat.zwave.commands.configurationv1.ConfigurationReport cmd) {
	def value = "when off"
	if (cmd.configurationValue[0] == 1) {value = "when on"}
	if (cmd.configurationValue[0] == 2) {value = "never"}
	[name: "indicatorStatus", value: value, displayed: false]
}

def on() {
	commands([
		zwave.basicV1.basicSet(value: 0xFF),
		zwave.basicV1.basicGet()
	])
}

def off() {
	commands([
		zwave.basicV1.basicSet(value: 0x00),
		zwave.basicV1.basicGet()
	])
}

def ping() {
	refresh()
}

def poll() {
	refresh()
}

def refresh() {
	command(zwave.basicV1.basicGet())
}

private command(hubitat.zwave.Command cmd) {
	if ((zwaveInfo.zw == null && state.sec != 0) || zwaveInfo?.zw?.contains("s")) {
		zwave.securityV1.securityMessageEncapsulation().encapsulate(cmd).format()
	} else {
		cmd.format()
	}
}

private commands(commands, delay = 200) {
	delayBetween(commands.collect { command(it) }, delay)
}
