/* Zwave Radio Thermostat Hubitat driver
Hubitat driver for radio thermostat & Iris Thermostat
Radio Thermostat Company of America (RTC)

Supports
poll schedule, humidity,heat or cool only,C-wire,Diff,Recovery mode,Mans detection

______          _ _         _____ _                                   _        _   
| ___ \        | (_)       |_   _| |                                 | |      | |  
| |_/ /__ _  __| |_  ___     | | | |__   ___ _ __ _ __ ___   ___  ___| |_ __ _| |_ 
|    // _` |/ _` | |/ _ \    | | | '_ \ / _ \ '__| '_ ` _ \ / _ \/ __| __/ _` | __|
| |\ \ (_| | (_| | | (_) |   | | | | | |  __/ |  | | | | | | (_) \__ \ || (_| | |_ 
\_| \_\__,_|\__,_|_|\___/    \_/ |_| |_|\___|_|  |_| |_| |_|\___/|___/\__\__,_|\__|
                                                                                  


tested on:
CT-101 Iris version
CT-30e rev1
CT-30e

ZWAVE SPECIFIC_TYPE_THERMOSTAT_GENERAL_V2
===================================================================================================
 v5.1   08/05/2022 Changes to manufacturerSpecificGet. Heat or Cool Only working
 v4.8   08/04/2022 First major release out of beta working code.
 v3.3   07/30/2022 Improvements and debuging code.      
 v3.1   07/30/2022 Total rewrite of event storage and Parsing.
 v3.0   07/29/2022 Release - Major fixes and log conversion
 -----             Missing numbers are beta working local versions never released.
===================================================================================================

Paring info:
Bring hub to thermostat or thermostat to hub connect 24 volts AC between C and RC. (AC ONLY). 
Be sure you get paired in c-wire mode.



Totaly rewritten modern logging system. Its now easer to see what your thermostat has done in the past.
Events numbers added to log to detect what routines are throwing errors.


New chron to pull the temp and settings from some thermostats that will go to sleep.No need for routines.

Notes
CT101 will work in battery mode or c-wire mode. However I havent done much testing in battery mode.

Models with the Radio Thermostat Z-Wave USNAP Module RTZW-01 Require C-Wire or they will go to sleep
never to rewake. Some will take commands in sleep some wont. c-wire is required.
Do not even atempt to use battery mode.

Errors:
Everynow and then it throws a mfg id error. still working on this.
its been moved to config so it doesnt run on refreash


Supports humidity 

Total new rewrite of clock code.

Added new settings pulled from thermostat. 

Setting for HEAT or COOLING only.


: humidity 35 %
: Temp Diff Heat:4  Cool:4
: Fast recovery :economy 
: thermostatOperatingState - idle 
: thermostatFanMode - fanAuto 
: thermostatMode - cool 
: Set Points Match 80.0 = coolingSetpoint 80.0
: coolingSetpoint 80 F
: heatingSetpoint 65 F
: temperature 80.5 F
: C-Wire :TRUE PowerSouce :mains






Figured out from trial and error RTC formats to add other features.

Looked how commans were sent and received on many diffrent drivers from other brands.

Code cleanup and restructuring. 

Major sections of forked code not needed on hubitat were removed.

======================================================================================================
Copyright [2022] [tmaster winnfreenet.com]

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
=======================================================================================================



Some code used from several diffrent versions here

hubitat port http://www.apache.org/licenses/LICENSE-2.0
https://github.com/MarioHudds/hubitat/blob/master/Enhanced%20Z-Wave%20Thermostat
https://community.hubitat.com/t/port-enhanced-z-wave-thermostat-ct-100-w-humidity-and-time-update/4743


Modified version here 
https://github.com/motley74/SmartThingsPublic/blob/master/devicetypes/motley74/ct100-thermostat-custom.src/ct100-thermostat-custom.groovy

Modified version here
https://github.com/SmartThingsCommunity/SmartThingsPublic/blob/master/devicetypes/smartthings/ct100-thermostat.src/ct100-thermostat.groovy

Orginal old smartthings driver http://www.apache.org/licenses/LICENSE-2.0
https://community.smartthings.com/t/release-enhanced-z-wave-plus-thermostat-device-handler-honeywell-gocontrol-ct-linear-trane-mco-remotec/7284/
 */
//import groovy.json.JsonOutput
def clientVersion() {
    TheVersion="5.1"
 if (state.version != TheVersion){ 
     state.version = TheVersion
     configure() 
 }
}


metadata {

	definition (name: "Radio Thermostat Zwave", namespace: "tmastersmart", author: "tmaster", importUrl: "https://raw.githubusercontent.com/tmastersmart/hubitat-code/main/radio_thermostat_zwave.groovy") {
		capability "Actuator"
		capability "Configuration"
		capability "Polling"
		capability "Sensor"
		capability "Refresh"
        capability "Battery"
        capability "PowerSource"
        capability "Thermostat"
        capability "TemperatureMeasurement"
        capability "ThermostatMode"
        capability "ThermostatFanMode"
        capability "ThermostatSetpoint"
        capability "ThermostatCoolingSetpoint"
        capability "ThermostatHeatingSetpoint"
        capability "ThermostatOperatingState"
        capability "RelativeHumidityMeasurement"
        attribute "thermostatFanState", "string"
//attribute "supportedFanModes", "string"
        attribute "SetCool", "string"
        attribute "SetHeat", "string"
//		command "switchMode"
//		command "switchFanMode"
        command "unschedule"
        command "ReSetClock"
        command "uninstall"
//        command "updated"
		fingerprint deviceId: "0x08"
		fingerprint inClusters: "0x43,0x40,0x44,0x31"
        
        

        
        fingerprint inClusters: "0x20,0x87,0x72,0x31,0x40,0x42,0x44,0x43,0x86"//                     * ct-30e Z-Wave USNAP Module RTZW-01
        fingerprint inClusters: "0x20,0x87,0x72,0x31,0x40,0x44,0x43,0x42,0x86,0x70,0x80,0x88" //     * ct-30 private lable alarm
        fingerprint inClusters: "0x20,0x81,0x87,0x72,0x31,0x40,0x44,0x43,0x42,0x86,0x70,0x80,0x88" //* ct-30e rev v1 Z-Wave USNAP Module RTZW-01
        
        fingerprint inClusters: "0x20,0x81,0x87,0x72,0x31,0x40,0x42,0x44,0x45,0x43,0x86,0x70,0x80,0x85,0x60" //      ct-100 & (ct-101 iris)
        fingerprint inClusters: "0x20,0x81,0x87,0x72,0x31,0x40,0x42,0x44,0x45,0x43,0x86,0x70,0x80,0x85,0x5D,0x60"//  ct-101
// hubitat ignores deviceJoinName only included for ref notes        
       fingerprint type:"0806", mfr:"0098", prod:"1E10", model:"0158",manufacturer: "Radio Thermostat", deviceJoinName:"CT-30 Radio Thermostat" 
       fingerprint type:"0806", mfr:"0098", prod:"0000", model:"0000",manufacturer: "Radio Thermostat", deviceJoinName:"CT-30 Radio Thermostat"
       fingerprint type:"0806", mfr:"0098", prod:"1E12", model:"015C",manufacturer: "Radio Thermostat", deviceJoinName:"CT-30e Radio Thermostat"
       fingerprint type:"0806", mfr:"0098", prod:"1E12", model:"015E",manufacturer: "Radio Thermostat", deviceJoinName:"CT-30 v1 Radio Thermostat"
       fingerprint type:"0806", mfr:"0098", prod:"0001", model:"001E",manufacturer: "Radio Thermostat", deviceJoinName:"CT-30e Radio Thermostat" 
       fingerprint type:"0806", mfr:"0098", prod:"0001", model:"0000",manufacturer: "Radio Thermostat", deviceJoinName:"CT-30 Z-Wave Thermostat" 
       fingerprint type:"0806", mfr:"0098", prod:"0001", model:"00FF",manufacturer: "Radio Thermostat", deviceJoinName:"CT-30 Z-Wave Thermostat" 
       fingerprint type:"0806", mfr:"0098", prod:"0001", model:"00FF",manufacturer: "Radio Thermostat", deviceJoinName:"CT-30 Z-Wave Thermostat"
       fingerprint type:"0806", mfr:"0098", prod:"2002", model:"0100",manufacturer: "Radio Thermostat", deviceJoinName:"CT-32 Z-Wave Thermostat" 
       fingerprint type:"0806", mfr:"0098", prod:"3200", model:"015E",manufacturer: "Radio Thermostat", deviceJoinName:"CT-50 Filtrete 3M-50 Thermostat" 
       fingerprint type:"0806", mfr:"0098", prod:"5003", model:"0109",manufacturer: "Radio Thermostat", deviceJoinName:"CT-80 Radio Thermostat"
       fingerprint type:"0806", mfr:"0098", prod:"5003", model:"01FD",manufacturer: "Radio Thermostat", deviceJoinName:"CT-80 Radio Thermostat" 
       fingerprint type:"0806", mfr:"0098", prod:"6401", model:"0107",manufacturer: "Radio Thermostat", deviceJoinName:"CT-100 Radio Thermostat"
       fingerprint type:"0806", mfr:"0098", prod:"6401", model:"0106",manufacturer: "Radio Thermostat", deviceJoinName:"CT-100 Vivint Thermostat"
       fingerprint type:"0806", mfr:"0098", prod:"6402", model:"0100",manufacturer: "Radio Thermostat", deviceJoinName:"CT-100 Plus Radio Thermostat"
       fingerprint type:"0806", mfr:"0098", prod:"6501", model:"00FD",manufacturer: "Radio Thermostat", deviceJoinName:"CT-101 Radio Thermostat"
       fingerprint type:"0806", mfr:"0098", prod:"6501", model:"000B",manufacturer: "Radio Thermostat", deviceJoinName:"CT-101 Lowes Thermostat"
       fingerprint type:"0806", mfr:"0098", prod:"6501", model:"000C",manufacturer: "Radio Thermostat", deviceJoinName:"CT-101 Iris Thermostat" 
        

// Tested on.... 
// fingerprint mfr:0098 prod:6501 model:000C ct101 Iris versio-Wave USNAP Module RTZW-01 n
// fingerprint mfr:0098 prod:1E12 model:015C ct30e rev v1 C-wire report    
// fingerprint mfr:0098 prod:0001 model:001E ct30e  Displays REMOTE CONTROL box when paired - No c-wire report         
	}
}
preferences {
    
    input name: "infoLogging",  type: "bool", title: "Enable info logging", description: "Recomended low level" ,defaultValue: true
	input name: "debugLogging", type: "bool", title: "Enable debug logging", description: "MED level Debug" ,defaultValue: false
	input name: "traceLogging", type: "bool", title: "Enable trace logging", description: "Insane HIGH level", defaultValue: false
    input name: "onlyMode", type: "enum", title: "Mode Bypass", description: "Heat or Cool only mode",  options: ["off", "heatonly","coolonly"], defaultValue: "off",required: true 
    input name: "autocorrect", type: "bool", title: "Auto Correct setpoints", description: "Keep thermostat settings matching hub (this will overide local changes)", defaultValue: false,required: true
    input(  "autocorrectNum", "number", title: "Auto Correct errors", description: "send auto corect after number of errors detected. ", defaultValue: 3,required: true)
    input(  "polling", "number", title: "Polling", description: "Mins between poll. Must config after changing", defaultValue: 15,required: true)

}

def installed(){
logging("${device} : Radio Thermostat Paired!", "info")
configure()
}

def uninstall() {
 
	unschedule()
    state.remove("pendingRefresh")
    state.remove("precision")
	state.remove("scale")
    state.remove("size")
    state.remove("version") 
    
	state.remove("supportedFanModes")
    state.remove("supportedModes")
	state.remove("lastbatt")
	state.remove("LastTimeSet")
	state.remove("lastTriedMode") 
    state.remove("lastTriedFanMode")
	state.remove("lastClockSet")
    state.remove("lastBattery")
	state.remove("lastMode")
    
	state.remove("lastBatteryGet")
	state.remove("lastOpState")
    
    state.remove("initialized")
    state.remove("configUpdated")
    state.remove("model") 
    state.remove("icon")
       
    
    state.remove("fingerprint")
    state.remove("model")
    state.remove("setCool")
    state.remove("setHeat")
    state.remove("cwire")

removeDataValue("thermostatSetpoint")    
removeDataValue("SetCool")
removeDataValue("coolingSetpoint")    
removeDataValue("SetHeat")   

// Clear crap from other drivers 
updateDataValue("hardwareVersion", "")    
updateDataValue("protocolVersion", "")
updateDataValue("lastRunningMode", "")
updateDataValue("zwNNUR", "")
    
    
logging("${device} : Uninstalled", "info")   
}

def configure() {
    unschedule()
    logging("${device} : Configure Driver v${state.version}", "info")
	device.updateSetting("infoLogging",[value:"true",type:"bool"])
	device.updateSetting("debugLogging",[value:"false",type:"bool"])
	device.updateSetting("traceLogging",[value:"false",type:"bool"])
	updated()
    state.cwire =0 
    delayBetween([
        zwave.manufacturerSpecificV2.manufacturerSpecificGet().format(),// fingerprint
		zwave.thermostatModeV2.thermostatModeSupportedGet().format(),
        zwave.thermostatFanModeV3.thermostatFanModeSupportedGet().format(), 
        zwave.configurationV2.configurationSet(parameterNumber: 4, size: 1, configurationValue: [2]).format(), // cwire enabled
        zwave.configurationV2.configurationGet(parameterNumber: 4).format(), // is cwire 1=true 2=false
        zwave.configurationV2.configurationGet(parameterNumber: 9).format(), // is fast recovery on ? 1on 0 off
        zwave.configurationV2.configurationGet(parameterNumber: 8).format(), // is diff
        getBattery(), 
        setClock(), 
	], 2300)
}



def updated() {
    // Poll the device every x min
    
    if (polling <10) {polling=15}
    if (polling >59) {polling=45}
    
  
    
	int checkEveryMinutes = polling	
    
	randomSixty = Math.abs(new Random().nextInt() % 60)
	schedule("${randomSixty} 0/${checkEveryMinutes} * * * ? *", poll)
    logging("${device} :Setting Chron for poll ${randomSixty} 0/${checkEveryMinutes} * * * ? *", "info")

    loggingStatus()
	runIn(3600,debugLogOff)
	runIn(3500,traceLogOff)
    
    delayBetween([
    zwave.thermostatModeV2.thermostatModeGet().format(),// get mode
    zwave.sensorMultilevelV3.sensorMultilevelGet().format(), // current temperature 
    setClock()     
    ], 2300)    
//	refresh()

}



def pollDevice() {
    poll()
}
def refresh() {
    poll()
}
def poll() {

 
    clientVersion()
    logging("${device} : Poll E=Event# v${state.version}", "info")
    //zwave.sensorMultilevelV5.sensorMultilevelGet().format(), // testing
    //zwave.batteryV1.batteryGet().format(),
    //zwave.commands.versionv2.VersionReport
    //        zwave.thermostatModeV2.thermostatModeSupportedGet().format(),
//      zwave.manufacturerSpecificV2.manufacturerSpecificGet().format(),
    
	delayBetween([
		zwave.sensorMultilevelV3.sensorMultilevelGet().format(), // current temperature
        zwave.multiInstanceV1.multiInstanceCmdEncap(instance: 2).encapsulate(zwave.sensorMultilevelV2.sensorMultilevelGet()).format(), // CT-100/101 Humidity
		zwave.thermostatSetpointV1.thermostatSetpointGet(setpointType: 1).format(),
		zwave.thermostatSetpointV1.thermostatSetpointGet(setpointType: 2).format(),
		zwave.thermostatModeV2.thermostatModeGet().format(),
		zwave.thermostatFanModeV3.thermostatFanModeGet().format(),
		zwave.thermostatOperatingStateV1.thermostatOperatingStateGet().format(),
        zwave.configurationV2.configurationGet(parameterNumber: 9).format(), // is fast recovery
        zwave.configurationV2.configurationGet(parameterNumber: 8).format(), // is temp diff
        zwave.configurationV2.configurationGet(parameterNumber: 4).format(),// get cwire
		getBattery(), 
        setClock() 
	], 2300)
}
//        "TempReport":		[ Param: 1, Size: 1, Default: 2, Min: 0, Max: 4, Value: reverseValue(value) ].with { put('ParamValue', paramValue(value, get('Size'))); it },
//        "UtilityLock": 	[ Param: 3, Size: 1, Default: 0, Full: 2, Partial: 1, Disabled: 0 ],
//        "CWire": 			[ Param: 4, Size: 1, Default: 2, Enabled: 1, Disabled: 2 ],
//        "HumidityReport":	[ Param: 5, Size: 1, Default: 2, Min: 0, Max: 3, Value: reverseValue(value) ].with { put('ParamValue', paramValue(value, get('Size'))); it },
//        "EmergencyHeat": 	[ Param: 6, Size: 1, Default: 0, Enabled: 1, Disabled: 0 ],
//        "TempSwing":		[ Param: 7, Size: 1, Default: 2, Min: 1, Max: 8, Value: reverseValue(value) ].with { put('ParamValue', paramValue(value, get('Size'))); it },
//        "DiffTemp":			[ Param: 8, Size: 2, Default: 4, Min: 0, Max: 65535, Value: reverseValue(value) ].with { put('ParamValue', paramValue(value, get('Size'))); it },
//        "FastRecovery": 	[ Param: 9, Size: 1, Default: 2, Enabled: 1, Disabled: 2 ],
//        "SimpleUIMode": 	[ Param: 11, Size: 1, Default: 1, Enabled: 1, Disabled: 0 ],
//        "MulticastMode": 	[ Param: 12, Size: 1, Default: 0, Enabled: 1, Disabled: 0 ],
//
//    zwave.configurationV2.configurationSet(parameterNumber: 11, size: 1, configurationValue: 1) // simple UI enabled 1 on 2 off
//    zwave.configurationV2.configurationGet(parameterNumber: 11) 
//    zwave.configurationV2.configurationSet(parameterNumber: 4, size: 1, configurationValue: 2) // cwire enabled
//    zwave.configurationV2.configurationGet(parameterNumber: 4) 
//    zwave.configurationV2.configurationSet(parameterNumber: 9, size: 1, configurationValue: 1) // fast recovery 1 on 0 off
//    zwave.configurationV2.configurationGet(parameterNumber: 9).format() // is fast recovery on ? 1on 0 off

def parse(String description)
{
// 0x31:3  Sensor Multilevel <--
// 0x40:2  Mode
// 0x42:1  Operating State  <--
// 0x43:2  Setpoint   <--
// 0x44:3  Fan Mode   <--
// 0x45:1  Fan State
// 0x60:3  Multi channel 
// 0x70:2  Config
// 0x72:2  Manufacturer Specific
// 0x80:1  Battery
// 0x81:1  Clock
// 0x85:1  Association
// 0x86:1  Version
// 0x98:1  Security
   //def zwcmd = zwave.parse(description, [0x42:2, 0x43:2, 0x31: 2, 0x60: 3])
   CommandClassCapabilities = [0x31:3,0x40:2,0x42:1,0x43:2,0x44:3,0x45:1,0x60:3,0x70:2,0x72:2,0x80:1,0x81:1,0x85:1,0x86:1]   
   hubitat.zwave.Command map = zwave.parse(description, CommandClassCapabilities)
    logging("${device} : Raw [${description}]", "trace")
//    if (!result) {return null}
//    logging("${device} : Parse ${map}", "info")
	def result = [map]
    logging("${device} : Parse ${result}", "debug")
  
    if (map) { 
        zwaveEvent(map)
        return
    }
    
	result
}

// Event Generation
// event E1
// Termostat setpoints -------------------------------------------------------
def zwaveEvent(hubitat.zwave.commands.thermostatsetpointv2.ThermostatSetpointReport cmd)
{
    logging("${device} : received E1 ${cmd}", "debug")
    def cmdScale = cmd.scale == 1 ? "F" : "C"
	def map = [:]
	map.value = convertTemperatureIfNeeded(cmd.scaledValue, cmdScale, cmd.precision)
    map.name = "unknown"
    
	map.unit = getTemperatureScale()
	map.displayed = false
     
	switch (cmd.setpointType) {
		case 1:
			map.name = "heatingSetpoint"
           tempCheck = state.SetHeat
			break;
		case 2:
        if(heatonly == true){return}
			map.name = "coolingSetpoint"
           tempCheck = state.SetCool
//           tempCheck = "77.0"
			break;
		default:
        logging("${device} : error Unknown ${map.value}", "warn")
			return [:]
	}
    if (map.name != null){
//    logging("${device} : ${cmd.setpointType} ${map.value}", "trace") 
    logging("${device} : E1 ${map.name} ${map.value} ${cmdScale}", "info")
    sendEvent(name: map.name , value: map.value,unit: cmdScale,descriptionText:"Driver ${state.version}", isStateChange:true)    
    }
    tempCheck2 = map.value.toDouble() 
    if(tempCheck){ // needed in case of no last setpoints set
     if(tempCheck == tempCheck2){
      logging("${device} : E1 ${map.name} Set Points Match Last${tempCheck}=Current${tempCheck2}", "info")
//      state.error = 0 
      }
     if(tempCheck != tempCheck2){
         logging("${device} : E1 ${map.name} Last Point does not match Last${tempCheck}<>Currect${tempCheck2} Errors:${state.error} <<---", "warn") 
      state.error = state.error +1
         
         if (state.error >= autocorrectNum) {
           if (autocorrect == true){ // Set in config. optional
                 setCoolingSetpoint(state.SetCool)
                 setHeatingSetpoint(state.SetHeat)
                 state.error = 0
           }    
         }   
      }
   }// end if not null

// stored so we can use on send
	state.size = cmd.size
	state.scale = cmd.scale
	state.precision = cmd.precision
}
// E2 has been null null null
def zwaveEvent(hubitat.zwave.commands.sensormultilevelv2.SensorMultilevelReport cmd)
{
	logging("${device} : received E2 ${cmd}", "debug")
    def map = [:]
    map.displayed = true
    map.isStateChange = true
    if (cmd.sensorType == 0) {//E2 precision:0, scale:3, sensorType:0, sensorValue:[], size:0, scaledSensorValue:null)
        logging("${device} : E2 Empty no data", "info") // ct30 known to do this 
        return
    }
	if (cmd.sensorType == 1) {
		map.value = convertTemperatureIfNeeded(cmd.scaledSensorValue, cmd.scale == 1 ? "F" : "C", cmd.precision)
		map.unit = getTemperatureScale()
		map.name = "temperature"
	} else if (cmd.sensorType == 5) {
		map.value = cmd.scaledSensorValue
		map.unit = "%"
		map.name = "humidity"
	}
    if (map.value != null){
    logging("${device} : E2 ${map.name} ${map.value} ${map.unit}", "info")
    sendEvent(name:map.name ,value:map.value ,unit: map.unit, descriptionText:"Driver ${state.version}", isStateChange: true)
    }
    else {logging("${device} : E2 Unknown data ${cmd}", "warn")}
	map
}
// E3
def zwaveEvent(hubitat.zwave.commands.thermostatoperatingstatev1.ThermostatOperatingStateReport cmd)
{
	def map = [:]
    logging("${device} : received E3 ${cmd}", "debug")
    map.name = "thermostatOperatingState"
    map.value = "unknown"
	switch (cmd.operatingState) {
		case hubitat.zwave.commands.thermostatoperatingstatev1.ThermostatOperatingStateReport.OPERATING_STATE_IDLE:
			map.value = "idle"
			break
		case hubitat.zwave.commands.thermostatoperatingstatev1.ThermostatOperatingStateReport.OPERATING_STATE_HEATING:
			map.value = "heating"
			break
		case hubitat.zwave.commands.thermostatoperatingstatev1.ThermostatOperatingStateReport.OPERATING_STATE_COOLING:
			map.value = "cooling"
			break
		case hubitat.zwave.commands.thermostatoperatingstatev1.ThermostatOperatingStateReport.OPERATING_STATE_FAN_ONLY:
			map.value = "fan only"
			break
		case hubitat.zwave.commands.thermostatoperatingstatev1.ThermostatOperatingStateReport.OPERATING_STATE_PENDING_HEAT:
			map.value = "pending heat"
			break
		case hubitat.zwave.commands.thermostatoperatingstatev1.ThermostatOperatingStateReport.OPERATING_STATE_PENDING_COOL:
			map.value = "pending cool"
			break
		case hubitat.zwave.commands.thermostatoperatingstatev1.ThermostatOperatingStateReport.OPERATING_STATE_VENT_ECONOMIZER:
			map.value = "vent economizer"
			break
	}
	
    logging("${device} : E3 ${map.name} - ${map.value} ", "info")
    sendEvent(name: map.name, value: map.value,descriptionText: "Driver ${state.version}", isStateChange:true)
	map
}
// E4
def zwaveEvent(hubitat.zwave.commands.thermostatfanstatev1.ThermostatFanStateReport cmd) {
    logging("${device} : received E4 ${cmd}", "debug")
	def map = [:]
    map.name = "thermostatFanState"    
    map.value = "unknown"
	switch (cmd.fanOperatingState) {
		case 0:
			map.value = "idle"
			break
		case 1:
			map.value = "running"
			break
		case 2:
			map.value = "running high"
			break
	}
    logging("${device} : received E4 ${map.name} - ${map.value} ", "info")
    sendEvent(name: map.name, value: map.value,descriptionText: "Driver ${state.version}", isStateChange:true)
	map
}

def zwaveEvent(hubitat.zwave.commands.thermostatmodev2.ThermostatModeReport cmd) {
	def map = [:]
    logging("${device} : received E5 ${cmd}", "debug")
    map.name = "thermostatMode"
    map.value = "unknown"
	switch (cmd.mode) {
		case hubitat.zwave.commands.thermostatmodev2.ThermostatModeReport.MODE_OFF:
			map.value = "off"
			break
		case hubitat.zwave.commands.thermostatmodev2.ThermostatModeReport.MODE_HEAT:
			map.value = "heat"
			break
		case hubitat.zwave.commands.thermostatmodev2.ThermostatModeReport.MODE_AUXILIARY_HEAT:
			map.value = "emergency heat"
			break
		case hubitat.zwave.commands.thermostatmodev2.ThermostatModeReport.MODE_COOL:
			map.value = "cool"
			break
		case hubitat.zwave.commands.thermostatmodev2.ThermostatModeReport.MODE_AUTO:
			map.value = "auto"
			break
	}
	
    logging("${device} : E5 ${map.name} - ${map.value} ", "info")
    sendEvent(name: map.name, value: map.value,descriptionText: "Driver ${state.version}", isStateChange:true)
    
	map
}

def zwaveEvent(hubitat.zwave.commands.thermostatfanmodev3.ThermostatFanModeReport cmd) {
	def map = [:]
    logging("${device} : received E6 ${cmd}", "debug")
    map.name = "thermostatFanMode"
    map.value = "unknown"
	switch (cmd.fanMode) {
		case hubitat.zwave.commands.thermostatfanmodev3.ThermostatFanModeReport.FAN_MODE_AUTO_LOW:
			map.value = "fanAuto"
			break
		case hubitat.zwave.commands.thermostatfanmodev3.ThermostatFanModeReport.FAN_MODE_LOW:
			map.value = "fanOn"
			break
		case hubitat.zwave.commands.thermostatfanmodev3.ThermostatFanModeReport.FAN_MODE_CIRCULATION:// Dont thinmk rT supports this
			map.value = "fanCirculate"
			break
	}
	
    logging("${device} : E6 ${map.name} - ${map.value} ", "info")
    sendEvent(name: map.name, value: map.value,descriptionText: "Driver ${state.version}", isStateChange:true)
 
	map.displayed = false
	map
}

def zwaveEvent(hubitat.zwave.commands.thermostatmodev2.ThermostatModeSupportedReport cmd) {
	def supportedModes = ""
    logging("${device} : received E7 ${cmd}", "debug")
	if(cmd.off) { supportedModes += "off," }
	if(cmd.heat) { supportedModes += "heat," }
	if(cmd.auxiliaryemergencyHeat) { supportedModes += "emergency heat," }
    if(cmd.cool) { supportedModes += "cool," }
    if(cmd.auto) { supportedModes += "auto" }
    
 	if(onlyMode == "coolonly"){
       supportedModes = "" 
    if(cmd.off) { supportedModes += "off," }
    if(cmd.cool) { supportedModes += "cool" }
    }
 	if(onlyMode == "heatonly"){
       supportedModes = "" 
    if(cmd.off) { supportedModes += "off," }
	if(cmd.heat) { supportedModes += "heat," }
	if(cmd.auxiliaryemergencyHeat) { supportedModes += "emergency heat" }
    }        
        

	state.supportedModes = supportedModes
    logging("${device} : E7 supportedModes [${supportedModes}]", "info")
    sendEvent(name: "supportedThermostatModes", value: "[${supportedModes}]",descriptionText: supportedModes, isStateChange:true)

  
//    supportedThermostatModes : [off, heat, cool, auto, emergency heat]
    
}
// E8
def zwaveEvent(hubitat.zwave.commands.thermostatfanmodev3.ThermostatFanModeSupportedReport cmd) {
	def supportedFanModes = ""
    logging("${device} : received E8 ${cmd}", "debug")
	if(cmd.auto) { supportedFanModes += "fanAuto," }
	if(cmd.low) { supportedFanModes += "fanOn," }
	if(cmd.circulation) { supportedFanModes += "fanCirculate " } // not used

	state.supportedFanModes = supportedModes
    logging("${device} : E8 supportedFanModes[${supportedFanModes}]", "info")
    sendEvent(name: "supportedFanModes", value: "[${supportedModes}]",descriptionText: supportedModes, isStateChange:true)

  // supportedFanModes [fanAuto fanOn ]  
    
}
// these are untrapped log them...
def zwaveEvent(hubitat.zwave.commands.basicv1.BasicReport cmd) {
    logging("${device} : Received E9 ${cmd}", "debug")
}

def zwaveEvent(hubitat.zwave.commands.configurationv2.ConfigurationReport cmd) {
    logging("${device} : Received E10 ${cmd}", "debug")
// ConfigurationReport(parameterNumber: 8, size: 2, configurationValue: [4, 4], scaledConfigurationValue: 1028) Diff
   if (cmd.parameterNumber== 8){
       state.heatDiff = cmd.configurationValue[0] 
       state.coolDiff = cmd.configurationValue[1] 
       logging("${device} : E10 Temp Diff Heat:${state.heatDiff}  Cool:${state.coolDiff}", "info")
   }
// ConfigurationReport(parameterNumber: 9, size: 1, configurationValue: [2], scaledConfigurationValue: 2)  Fast Recovery
   if (cmd.parameterNumber== 9){
       state.fastrecovery = cmd.configurationValue[0] 
        if (state.fastrecovery == 1){logging("${device} : E10 Fast recovery :fast", "info")}
        if (state.fastrecovery == 2){logging("${device} : E10 Fast recovery :economy ", "info") }
   }    
//  ConfigurationReport(parameterNumber: 4, size: 1, configurationValue: [1], scaledConfigurationValue: 1)
    if (cmd.parameterNumber== 4){
//      state.cwire = cmd.configurationValue[0]
        state.cwire = cmd.scaledConfigurationValue
        if (state.cwire == 1){
            logging("${device} : E10 C-Wire :TRUE PowerSouce :mains", "info")
            sendEvent(name: "powerSource", value: "mains",descriptionText: "Driver ${state.version}", isStateChange: true)
        }
        if (state.cwire == 2){
            logging("${device} : E10 C-Wire :FALSE PowerSouce :battery", "info") 
            sendEvent(name: "powerSource", value: "battery",descriptionText: "Driver ${state.version}", isStateChange: true)
        }
    }
}

def zwaveEvent(hubitat.zwave.Command cmd ){
  logging("${device} : Received E11 (${cmd})", "debug")
//  if(cmd != null) { setModelUp(cmd)}  
//  if (cmd.contains("ManufacturerSpecificReport")){ setModelUp(cmd)}  
	def map = [:]
    map.name = "ManufacturerSpecificReport"
    map.mfr   = hubitat.helper.HexUtils.integerToHexString(cmd.manufacturerId, 2)
    map.model = hubitat.helper.HexUtils.integerToHexString(cmd.productId, 2)
    map.type  = hubitat.helper.HexUtils.integerToHexString(cmd.productTypeId, 2)
    logging("${device} : E11 fingerprint mfr:${map.mfr} prod:${map.type} model:${map.model}", "debug")
   
//   state.remove("fingerprint")
   state.model ="unknown"
    
    if (map.type=="1E12" | map.type=="1E10" | map.type=="0000" | map.type=="0001"){
      state.model ="CT30"  
      if (map.model=="000C") {state.model ="CT30e rev.01"}
    } 
    
    if (map.type=="2002" ){ state.model ="CT32"}    
    if (map.type=="3200" ){ state.model ="CT50"}
    if (map.type=="5003" ){ state.model ="CT80"}
    if (map.type=="6401" | map.type=="6402"){ state.model ="CT100"}   
    if (map.type=="6501"){   
      state.model ="CT101"
      if (map.model=="000B") {state.model ="CT101 iris"}
   }
    logging("${device} : E11 fingerprint ${state.model} [${map.mfr}-${map.type}-${map.model}] ", "info")
    if (!getDataValue("manufacturer")) {updateDataValue("manufacturer", map.mfr)}
    if (!getDataValue("brand")){        updateDataValue("brand", "Radio Thermostat")}
    
    if (state.model =="unknown"){
        state.fingerprint = "Report fingerprint [${map.mfr}-${map.type}-${map.model}]"
        return
    }  
       
    if (!getDataValue("model")){        updateDataValue("model", state.model)}
    if (!getDataValue("deviceId")){     updateDataValue("deviceId", map.model)}
    if (!getDataValue("deviceType")){   updateDataValue("deviceType", map.type)}
        
    
}


void zwaveEvent(hubitat.zwave.commands.versionv2.VersionReport cmd) {
    logging("${device} : Received E12 ${cmd}", "debug")
    device.updateDataValue("firmwareVersion", "${cmd.firmware0Version}.${cmd.firmware0SubVersion}")
    device.updateDataValue("protocolVersion", "${cmd.zWaveProtocolVersion}.${cmd.zWaveProtocolSubVersion}")
    device.updateDataValue("hardwareVersion", "${cmd.hardwareVersion}")
}




//def quickSetHeat(degrees) {
//	setHeatingSetpoint(degrees, 1000)
//}

def setHeatingSetpoint(degrees, delay = 30000) {
	setHeatingSetpoint(degrees.toDouble(), delay)
}

def setHeatingSetpoint(Double degrees, Integer delay = 30000) {
    if(onlyMode == "coolonly"){
       coolOnly()
       return
    } 
	def deviceScale = state.scale ?: 1
	def deviceScaleString = deviceScale == 2 ? "C" : "F"
    def locationScale = getTemperatureScale()
	def p = (state.precision == null) ? 1 : state.precision

    def convertedDegrees
    if (locationScale == "C" && deviceScaleString == "F") {
    	convertedDegrees = celsiusToFahrenheit(degrees)
    } else if (locationScale == "F" && deviceScaleString == "C") {
    	convertedDegrees = fahrenheitToCelsius(degrees)
    } else {
    	convertedDegrees = degrees
    }
    state.SetHeat = convertedDegrees
    logging("${device} : Set Heat Setpoint ${convertedDegrees} ${locationScale} ---  Reset Last to ${state.SetHeat}", "info")
    sendEvent(name: "SetHeat", value: convertedDegrees, unit:locationScale ,descriptionText: "Reset Last to ${state.SetHeat} Driver ${state.version}", isStateChange:true)
    sendEvent(name: "thermostatSetpoint", value: convertedDegrees, unit:locationScale ,descriptionText: "Reset Last to ${state.SetHeat} Driver ${state.version}", isStateChange:true)
    
     
    
    logging("${device} : Set (heat type=1) scale:${deviceScale}, precision:${p},  scaledValue:${convertedDegrees}", "trace")
	delayBetween([
		zwave.thermostatSetpointV1.thermostatSetpointSet(setpointType: 1, scale: deviceScale, precision: p, scaledValue: convertedDegrees).format(),
		zwave.thermostatSetpointV1.thermostatSetpointGet(setpointType: 1).format()
	], delay)
}

//def quickSetCool(degrees) {
//	setCoolingSetpoint(degrees, 10000)
//}

def setCoolingSetpoint(degrees, delay = 30000) {
    logging("${device} : Set Cool Setpoint ${degrees} delay:${delay}", "debug")
	setCoolingSetpoint(degrees.toDouble(), delay)
}

def setCoolingSetpoint(Double degrees, Integer delay = 30000) {
    if(onlyMode == "heatonly"){
       heatingOnly()
    }
	def deviceScale = state.scale ?: 1
	def deviceScaleString = deviceScale == 2 ? "C" : "F"
    def locationScale = getTemperatureScale()
	def p = (state.precision == null) ? 1 : state.precision

    def convertedDegrees
    if (locationScale == "C" && deviceScaleString == "F") {
    	convertedDegrees = celsiusToFahrenheit(degrees)
    } else if (locationScale == "F" && deviceScaleString == "C") {
    	convertedDegrees = fahrenheitToCelsius(degrees)
    } else {
    	convertedDegrees = degrees
    }
    state.SetCool = convertedDegrees
    logging("${device} : Set Cool Setpoint ${convertedDegrees} ${locationScale} ---Reset Last to ${state.SetCool}", "info")
    sendEvent(name: "SetCool", value: convertedDegrees, unit:locationScale ,descriptionText: "Reset Last to ${state.SetCool} Driver ${state.version}", isStateChange:true)
    sendEvent(name: "thermostatSetpoint", value: convertedDegrees, unit:locationScale ,descriptionText: "Reset Last to ${state.SetCool} Driver ${state.version}", isStateChange:true)

    
    
    logging("${device} :Set (cool type=2) scale:${deviceScale}, precision:${p},  scaledValue:${convertedDegrees}", "trace")

    
	delayBetween([
		zwave.thermostatSetpointV1.thermostatSetpointSet(setpointType: 2, scale: deviceScale, precision: p, scaledValue: convertedDegrees).format(),
		zwave.thermostatSetpointV1.thermostatSetpointGet(setpointType: 2).format()
	], delay)
 
}



def getDataByName(String name) {
	state[name] ?: device.getDataValue(name)
}



def setThermostatMode(String value) {
	delayBetween([
		zwave.thermostatModeV2.thermostatModeSet(mode: modeMap[value]).format(),
		zwave.thermostatModeV2.thermostatModeGet().format()
	], standardDelay)
}



def setThermostatFanMode(String value) {
	delayBetween([
		zwave.thermostatFanModeV3.thermostatFanModeSet(fanMode: fanModeMap[value]).format(),
		zwave.thermostatFanModeV3.thermostatFanModeGet().format()
	], standardDelay)
}

// -------------------------------------mode setting ------------------
//   "onlyMode" ["off", "heatonly","coolonly"] 
void coolOnly(){
logging("${device} : Cooling Only Heat disabled", "info") 
state.setHeat = ""    
removeDataValue("heatingSetpoint")    
removeDataValue("SetHeat")
  state.remove("setHeat")    
}
void heatingOnly(){
logging("${device} : Heating Only Cool disabled", "info") 
state.setCool = ""    
removeDataValue("coolingSetpoint")    
removeDataValue("SetCool")
  state.remove("setCool")    
}
def off() {
    logging("${device} : Set mode OFF", "info")
	delayBetween([
		zwave.thermostatModeV2.thermostatModeSet(mode: 0).format(),
		zwave.thermostatModeV2.thermostatModeGet().format()
	], standardDelay)
}
def heat() {
        //   "onlyMode" ["off", "heatonly","coolonly"]     
    if(onlyMode == "coolonly"){
       coolOnly()
       return
    } 
    logging("${device} : Set mode HEAT", "info")
	delayBetween([
		zwave.thermostatModeV2.thermostatModeSet(mode: 1).format(),
		zwave.thermostatModeV2.thermostatModeGet().format()
	], standardDelay)
}

def emergencyHeat() {
    //   "onlyMode" ["off", "heatonly","coolonly"]     
    if(onlyMode == "coolonly"){
       coolOnly()
       return
    } 
    logging("${device} : Set Mode Emergency heat", "info")
	delayBetween([
		zwave.thermostatModeV2.thermostatModeSet(mode: 4).format(),
		zwave.thermostatModeV2.thermostatModeGet().format()
	], standardDelay)
}

def cool() {
//   "onlyMode" ["off", "heatonly","coolonly"]     
    if(onlyMode == "heatonly"){
       heatingOnly()
       return
    } 

    
    logging("${device} : Set Mode Cool", "info")
	delayBetween([
		zwave.thermostatModeV2.thermostatModeSet(mode: 2).format(),
		zwave.thermostatModeV2.thermostatModeGet().format()
	], standardDelay)
}

def auto() {
//   "onlyMode" ["off", "heatonly","coolonly"]     
    if(onlyMode == "heatonly"){
       heatingOnly()
       return
    } 
    logging("${device} : Set Mode Auto", "info")
	delayBetween([
		zwave.thermostatModeV2.thermostatModeSet(mode: 3).format(),
		zwave.thermostatModeV2.thermostatModeGet().format()
	], standardDelay)
}

def fanOn() {
    logging("${device} : Set Fan ON", "info")
	delayBetween([
		zwave.thermostatFanModeV3.thermostatFanModeSet(fanMode: 1).format(),
		zwave.thermostatFanModeV3.thermostatFanModeGet().format()
	], standardDelay)
}

def fanAuto() {
    logging("${device} : Set Fan Auto", "info")
	delayBetween([
		zwave.thermostatFanModeV3.thermostatFanModeSet(fanMode: 0).format(),
		zwave.thermostatFanModeV3.thermostatFanModeGet().format()
	], standardDelay)
}

def fanCirculate() {
    logging("${device} : Set Fan Circulate", "info")
	delayBetween([
		zwave.thermostatFanModeV3.thermostatFanModeSet(fanMode: 6).format(),
		zwave.thermostatFanModeV3.thermostatFanModeGet().format()
	], standardDelay)
}

private getStandardDelay() {
	1000
}

// CUSTOMIZATIONS
def zwaveEvent(hubitat.zwave.commands.multiinstancev1.MultiInstanceCmdEncap cmd) {   
//    Decapsulate command 
    def encapsulatedCommand = cmd.encapsulatedCommand([0x31: 2])
    if (encapsulatedCommand) {
        logging("${device} : E14 ${encapsulatedCommand}", "debug")
        return zwaveEvent(encapsulatedCommand)
    }
}
// E15
def zwaveEvent(hubitat.zwave.commands.batteryv1.BatteryReport cmd) {
    logging("${device} : E15 battery  ${cmd}", "debug")
    def nowTime = new Date().time
    state.lastBatteryGet = nowTime
    def map = [ name: "battery", unit: "%" ]
    map.displayed = true
    map.isStateChange = true
    if (cmd.batteryLevel == 0xFF){ // I have never seen this but its in the spec.
        logging("${device} : ---- Power Restored ----", "info")
        sendEvent(name: "powerSource", value: "mains",descriptionText: "Driver ${state.version}", isStateChange: true)
        return
    }

    map.value = cmd.batteryLevel
    test = cmd.batteryLevel
    if (state.cwire == 1){extra="Mains power"}
    if (state.cwire == 2){extra="Battery power"}
    if (state.cwire == 0){extra="Unknown power"} 
                    
    logging("${device} : E15 battery ${test}% ${extra}", "info")
    sendEvent(name: map.name, value: test,unit: map.unit, descriptionText: "${extra} Driver ${state.version}", isStateChange:true)

    map
}

private getBattery() {	
	def nowTime = new Date().time
	def ageInMinutes = state.lastBatteryGet ? (nowTime - state.lastBatteryGet)/60000 : 1440
//    log.debug "Battery report age: ${ageInMinutes} minutes"
    if (ageInMinutes <60){ logging("${device} : Skipping Bat Fetch. age:${ageInMinutes} min", "debug")}
    if (ageInMinutes >= 60) {
        state.lastBatteryGet = nowTime
        logging("${device} : Requesting Battery age:${ageInMinutes} min", "debug")
		zwave.batteryV1.batteryGet().format()
    } else "delay 87"
}
private ReSetClock(){
    state.lastClockSet = 1500
    setClock()
}


// Auto set clock code (improved)
// Day is not visiable in ct101 but is on ct30
private setClock() {

    def nowTime = new Date().time
	def ageInMinutes = state.lastClockSet ? (nowTime - state.lastClockSet)/60000 : 1440

    if (ageInMinutes >= 60) { // once a hr
		state.lastClockSet = nowTime
        def nowCal = Calendar.getInstance(location.timeZone) // get current location timezone
        state.LastTimeSet = "${nowCal.getTime().format("EEE MMM dd yyyy HH:mm:ss z", location.timeZone)}"
        setDay(nowCal.get(Calendar.DAY_OF_WEEK)) // gives us weekday name
        theTime ="${weekday} ${nowCal.get(Calendar.HOUR_OF_DAY)}:${nowCal.get(Calendar.MINUTE)}"

        weekdayZ = nowCal.get(Calendar.DAY_OF_WEEK) -1 // Gives us zwave weekday
        if (weekdayZ <1){weekdayZ = 7} // rotate to sat
        logging("${device} : Adjusting clock ${theTime} 24hr", "info")
        sendEvent(name: "SetClock", value: theTime, descriptionText: "Driver ${state.version}",displayed: true, isStateChange:true)

        delayBetween([
		zwave.clockV1.clockSet(hour: nowCal.get(Calendar.HOUR_OF_DAY), minute: nowCal.get(Calendar.MINUTE), weekday: weekdayZ).format(),
        zwave.clockV1.clockGet().format()
	], standardDelay)
    } else "delay 87"
}

void setDay(day){
    if (day==0){weekday="Saturday"}
    if (day==1){weekday="Sunday"}  
    if (day==2){weekday="Monday"}
    if (day==3){weekday="Tuesday"}
    if (day==4){weekday="Wednesday."}
    if (day==5){weekday="Thursday"}
    if (day==6){weekday="Friday"}
    if (day==7){weekday="Saturday"}
    if (day==8){weekday="Sunday"}
    
}
// The zwave day is 1 less than the hub
def zwaveEvent(hubitat.zwave.commands.clockv1.ClockReport cmd) {
    setDay(cmd.weekday+1)
    def nowCal = Calendar.getInstance(location.timeZone) // get current location timezone
    Timecheck = "${nowCal.getTime().format("EEE MMM dd yyyy HH:mm:ss z", location.timeZone)}"
    setclock= false 
    if (cmd.weekday != nowCal.get(Calendar.DAY_OF_WEEK)-1 ){ setclock=true}
    if (cmd.hour    != nowCal.get(Calendar.HOUR_OF_DAY)){setclock=true}
    if (cmd.minute  != nowCal.get(Calendar.MINUTE)){     setclock=true}
    if (setclock == false) {logging("${device} : E16 Receiving clock ${weekday} ${cmd.hour}:${cmd.minute} 24hr ok", "info")}
    if (setclock == true) {logging("${device} : E16 Receiving clock ${weekday} ${cmd.hour}:${cmd.minute} 24hr (out of sync)", "warn")}
}

// is this needed
def getModeMap() { 
    [
	"off": 0,
	"heat": 1,
	"cool": 2,
	"auto": 3,
	"emergency heat": 4
]
}

def getFanModeMap() {
    [
    "auto": 0,
    "on": 1,
    "circulate": 6
        ]
}
def modes() {
	[
        "off",
        "heat",
        "cool",
       "auto",
       "emergency heat"
    ]
}


void loggingStatus() {
	log.info  "${device} : Info  Logging : ${infoLogging == true}"
	log.debug "${device} : Debug Logging : ${debugLogging == true}"
	log.trace "${device} : Trace Logging : ${traceLogging == true}"
}


void traceLogOff(){
	device.updateSetting("traceLogging",[value:"false",type:"bool"])
	log.trace "${device} : Trace Logging : Automatically Disabled"
}


void debugLogOff(){
	device.updateSetting("debugLogging",[value:"false",type:"bool"])
	log.debug "${device} : Debug Logging : Automatically Disabled"
}



private boolean logging(String message, String level) {
	boolean didLog = false
	if (level == "error") {
		log.error "$message"
		didLog = true
	}
	if (level == "warn") {
		log.warn "$message"
		didLog = true
	}
	if (traceLogging && level == "trace") {
		log.trace "$message"
		didLog = true
	}
	if (debugLogging && level == "debug") {
		log.debug "$message"
		didLog = true
	}
	if (infoLogging && level == "info") {
		log.info "$message"
		didLog = true
	}
	return didLog
}



