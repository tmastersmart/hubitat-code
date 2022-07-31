/* Zwave Radio Thermostat Hubitat driver

Polling schedule pull current temp for 
thermostats that go to sleep for hrs.

May work on others but tested on 
CT-101 Iris version
CT-30e   
Polling only works on C wire mode.


 v3.3   07/30/2022 Improvements and debuging code.
                   
 v3.1   07/30/2022 Total rewrite of event storage and Parsing.
 v3.0   07/29/2022 Release - Major fixes and log conversion

===================================================================================================
Forked from a port on Hubitat
https://community.hubitat.com/t/port-enhanced-z-wave-thermostat-ct-100-w-humidity-and-time-update/4743


Orginal Forked from a old smartthings driver
https://community.smartthings.com/t/release-enhanced-z-wave-plus-thermostat-device-handler-honeywell-gocontrol-ct-linear-trane-mco-remotec/7284/
https://github.com/MarioHudds/hubitat/blob/master/Enhanced%20Z-Wave%20Thermostat

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
 */

def clientVersion() {
    TheVersion="3.3"
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
//      capability "PowerSource"
        capability "Thermostat"
        capability "TemperatureMeasurement"
        capability "ThermostatMode"
        capability "ThermostatFanMode"
//      capability "ThermostatSetpoint"
        capability "ThermostatCoolingSetpoint"
        capability "ThermostatHeatingSetpoint"
        capability "ThermostatOperatingState"
        capability "RelativeHumidityMeasurement"

        
        
        
        
        
        
        
        
		attribute "thermostatFanState", "string"
        attribute "SetCool", "string"
        attribute "SetHeat", "string"
        
		command "switchMode"
		command "switchFanMode"
        command "unschedule"
        command "ReSetClock"
        command "uninstall"
//        command "updated"

        

		fingerprint deviceId: "0x08"
		fingerprint inClusters: "0x43,0x40,0x44,0x31"
       fingerprint type:"0806", mfr:"0098", prod:"6401", model:"0107",manufacturer: "Radio Thermostat", deviceJoinName:"CT-100 Radio Thermostat",inClusters: "0x20,0x81,0x87,0x72,0x31,0x40,0x42,0x44,0x45,0x43,0x86,0x70,0x80,0x85,0x60"
       fingerprint type:"0806", mfr:"0098", prod:"6401", model:"0106",manufacturer: "Radio Thermostat", deviceJoinName:"CT-100 Vivint Thermostat",inClusters: "0x20,0x81,0x87,0x72,0x31,0x40,0x42,0x44,0x45,0x43,0x86,0x70,0x80,0x85,0x60"
       fingerprint type:"0806", mfr:"0098", prod:"6501", model:"00FD",manufacturer: "Radio Thermostat", deviceJoinName:"CT-101 Radio Thermostat" ,inClusters: "0x20,0x81,0x87,0x72,0x31,0x40,0x42,0x44,0x45,0x43,0x86,0x70,0x80,0x85,0x60"
       fingerprint type:"0806", mfr:"0098", prod:"6501", model:"000B",manufacturer: "Radio Thermostat", deviceJoinName:"CT-101 Lowes Thermostat",inClusters: "0x20,0x81,0x87,0x72,0x31,0x40,0x42,0x44,0x45,0x43,0x86,0x70,0x80,0x85,0x60"
       fingerprint type:"0806", mfr:"0098", prod:"1E12", model:"015C",manufacturer: "Radio Thermostat", deviceJoinName:"CT30e Radio Thermostat", inClusters: "0x20,0x81,0x87,0x72,0x31,0x40,0x44,0x43,0x42,0x86,0x70,0x80,0x88"
       
        
	}
}
preferences {
    
    input name: "infoLogging",  type: "bool", title: "Enable info logging", description: "Recomended low level" ,defaultValue: true
	input name: "debugLogging", type: "bool", title: "Enable debug logging", description: "MED level Debug" ,defaultValue: false
	input name: "traceLogging", type: "bool", title: "Enable trace logging", description: "Insane HIGH level", defaultValue: false
    input name: "cwire", type: "bool", title: "Are you using C Wire", description: "If using C Wire changes polling and bat reports", defaultValue: true
    
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
	state.remove("lastBatteryGet")
	state.remove("lastOpState")
	state.remove("supportedFanModes")
	state.remove("lastbatt")
	state.remove("LastTimeSet")
	state.remove("lastOpState")    
	state.remove("lastClockSet")
	state.remove("size")
    state.remove("supportedFanModes")
	state.remove("lastBattery")
	state.remove("LastTimeSet")
	state.remove("lastOpState")    
	state.remove("lastClockSet")
	state.remove("scale")
    state.remove("initialized")
    state.remove("configUpdated")
    state.remove("lastMode")   
    state.remove("version")    
    state.remove("supportedModes")

removeDataValue("thermostatSetpoint")
removeDataValue("SetCool")
removeDataValue("SetHeat")    
    
logging("${device} : Uninstalled", "info")   
}

def configure() {
    unschedule()
    updateDataValue("brand", "Radio Thermostat")
    logging("${device} : Configure Driver v${state.version}", "info")
	device.updateSetting("infoLogging",[value:"true",type:"bool"])
	device.updateSetting("debugLogging",[value:"true",type:"bool"])
	device.updateSetting("traceLogging",[value:"false",type:"bool"])
	updated()
    delayBetween([
		zwave.thermostatModeV2.thermostatModeSupportedGet().format(),
	], 2300)
}



def updated() {
    // Poll the device every x min
    
	int checkEveryMinutes = polling		
	randomSixty = Math.abs(new Random().nextInt() % 60)
	schedule("${randomSixty} 0/${checkEveryMinutes} * * * ? *", poll)
    logging("${device} :Setting Chron for poll ${randomSixty} 0/${checkEveryMinutes} * * * ? *", "info")

    loggingStatus()
	runIn(3600,debugLogOff)
	runIn(3500,traceLogOff)
	refresh()

}



def pollDevice() {
    poll()
}
def refresh() {
    poll()
}
def poll() {
    if (!cwire) {  
        if (polling <60){
            polling = 60
        }
    }
    clientVersion()
    logging("${device} : Poll Driver v${state.version}", "info")
    //zwave.sensorMultilevelV5.sensorMultilevelGet().format(), // testing
	delayBetween([
        
		zwave.sensorMultilevelV3.sensorMultilevelGet().format(), // current temperature
		zwave.thermostatSetpointV1.thermostatSetpointGet(setpointType: 1).format(),
		zwave.thermostatSetpointV1.thermostatSetpointGet(setpointType: 2).format(),
		zwave.thermostatModeV2.thermostatModeGet().format(),
		zwave.thermostatFanModeV3.thermostatFanModeGet().format(),
		zwave.thermostatOperatingStateV1.thermostatOperatingStateGet().format(),
		getBattery(), // CUSTOMIZATION
        setClock(), // CUSTOMIZATION
        zwave.multiInstanceV1.multiInstanceCmdEncap(instance: 2).encapsulate(zwave.sensorMultilevelV2.sensorMultilevelGet()).format(), // CT-100/101 Customization for Humidity
	], 2300)
}
//    Newer sample code To replace above
//    List<hubitat.zwave.Command> cmds=[]
//    cmds.add(zwave.batteryV1.batteryGet())
//    cmds.add(zwave.sensorMultilevelV5.sensorMultilevelGet(sensorType: 1, scale: configParam2==0?0:1))
//    cmds.add(zwave.sensorMultilevelV5.sensorMultilevelGet(sensorType: 5, scale: 0))
//    cmds.add(zwave.thermostatFanModeV3.thermostatFanModeGet())
//    cmds.add(zwave.thermostatFanStateV1.thermostatFanStateGet())
//    cmds.add(zwave.thermostatModeV2.thermostatModeGet())
//    cmds.add(zwave.thermostatOperatingStateV1.thermostatOperatingStateGet())
//    cmds.add(zwave.thermostatSetpointV2.thermostatSetpointGet(setpointType: 1))
//    cmds.add(zwave.thermostatSetpointV2.thermostatSetpointGet(setpointType: 2))
//    sendToDevice(cmds)










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

   CommandClassCapabilities = [0x31:3,0x40:2,0x42:1,0x43:2,0x44:3,0x45:1,0x60:3,0x70:2,0x72:2,0x80:1,0x81:1,0x85:1,0x86:1,0x98:1,]   
   hubitat.zwave.Command map = zwave.parse(description, CommandClassCapabilities)
    logging("${device} : Raw [${description}]", "trace")
    if (!map) {
       logging("${device} : Parse NULL", "trace")
		return null
	}
    
	def result = [map]
    logging("${device} : Parse ${map}", "debug")
    if (map) { 
        zwaveEvent(map)
        return
    }
    
// we should never get here 
//========================================    
    
    logging("${device} : Parse Error Map=${map} ${description}", "warn")
	result
}

// Event Generation
def zwaveEvent(hubitat.zwave.commands.thermostatsetpointv2.ThermostatSetpointReport cmd)
{
// java.lang.NumberFormatException: null on line 272 (method parse)   
    def cmdScale = cmd.scale == 1 ? "F" : "C"
	def map = [:]
	map.value = convertTemperatureIfNeeded(cmd.scaledValue, cmdScale, cmd.precision)
    map.name = "unknown"
    logging("${device} :Event Generation - ${cmd.setpointType} ${map.value}", "trace")
	map.unit = getTemperatureScale()
	map.displayed = false
	switch (cmd.setpointType) {
		case 1:
			map.name = "heatingSetpoint"
			break;
		case 2:
			map.name = "coolingSetpoint"
			break;
		default:
        logging("${device} : error Unknown ${map.value}", "info")
			return [:]
	}
    logging("${device} : ${map.name} ${map.value} ${cmdScale}", "info")
    sendEvent(name: map.name , value: map.value,unit: cmdScale,descriptionText:"Driver ${state.version}", isStateChange:true)    
    
	// So we can respond with same format
	state.size = cmd.size
	state.scale = cmd.scale
	state.precision = cmd.precision
	map
}

def zwaveEvent(hubitat.zwave.commands.sensormultilevelv2.SensorMultilevelReport cmd)
{
	def map = [:]
    map.displayed = true
    map.isStateChange = true
	if (cmd.sensorType == 1) {
		map.value = convertTemperatureIfNeeded(cmd.scaledSensorValue, cmd.scale == 1 ? "F" : "C", cmd.precision)
		map.unit = getTemperatureScale()
		map.name = "temperature"
	} else if (cmd.sensorType == 5) {
		map.value = cmd.scaledSensorValue
		map.unit = "%"
		map.name = "humidity"
	}
    logging("${device} : ${map.name} ${map.value} ${map.unit}", "info")
    sendEvent(name:map.name ,value:map.value ,unit: map.unit, descriptionText:"Driver ${state.version}", isStateChange: true)

	map
}

def zwaveEvent(hubitat.zwave.commands.thermostatoperatingstatev1.ThermostatOperatingStateReport cmd)
{
	def map = [:]
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
	
    logging("${device} : ${map.name} - ${map.value} ", "info")
    sendEvent(name: map.name, value: map.value,descriptionText: "Driver ${state.version}", isStateChange:true)
	map
}

def zwaveEvent(hubitat.zwave.commands.thermostatfanstatev1.ThermostatFanStateReport cmd) {
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
    logging("${device} : ${map.name} - ${map.value} ", "info")
    sendEvent(name: map.name, value: map.value,descriptionText: "Driver ${state.version}", isStateChange:true)
	map
}

def zwaveEvent(hubitat.zwave.commands.thermostatmodev2.ThermostatModeReport cmd) {
	def map = [:]
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
	
    logging("${device} : ${map.name} - ${map.value} ", "info")
    sendEvent(name: map.name, value: map.value,descriptionText: "Driver ${state.version}", isStateChange:true)
    
	map
}

def zwaveEvent(hubitat.zwave.commands.thermostatfanmodev3.ThermostatFanModeReport cmd) {
	def map = [:]
    map.name = "thermostatFanMode"
    map.value = "unknown"
	switch (cmd.fanMode) {
		case hubitat.zwave.commands.thermostatfanmodev3.ThermostatFanModeReport.FAN_MODE_AUTO_LOW:
			map.value = "fanAuto"
			break
		case hubitat.zwave.commands.thermostatfanmodev3.ThermostatFanModeReport.FAN_MODE_LOW:
			map.value = "fanOn"
			break
		case hubitat.zwave.commands.thermostatfanmodev3.ThermostatFanModeReport.FAN_MODE_CIRCULATION:
			map.value = "fanCirculate"
			break
	}
	
    logging("${device} : ${map.name} - ${map.value} ", "info")
    sendEvent(name: map.name, value: map.value,descriptionText: "Driver ${state.version}", isStateChange:true)
 
	map.displayed = false
	map
}

def zwaveEvent(hubitat.zwave.commands.thermostatmodev2.ThermostatModeSupportedReport cmd) {
	def supportedModes = ""
	if(cmd.off) { supportedModes += "off " }
	if(cmd.heat) { supportedModes += "heat " }
	if(cmd.auxiliaryemergencyHeat) { supportedModes += "emergency heat " }
	if(cmd.cool) { supportedModes += "cool " }
	if(cmd.auto) { supportedModes += "auto " }

	state.supportedModes = supportedModes
    logging("${device} : supportedModes [${supportedModes}]", "info")
    sendEvent(name: "supportedThermostatModes", value: "[${supportedModes}]",descriptionText: "Driver ${state.version}", isStateChange:true)

//    supportedModes- off heat cool    
//    supportedThermostatModes : [off, heat, cool, auto, emergency heat]
    
}

def zwaveEvent(hubitat.zwave.commands.thermostatfanmodev3.ThermostatFanModeSupportedReport cmd) {
	def supportedFanModes = ""
	if(cmd.auto) { supportedFanModes += "fanAuto " }
	if(cmd.low) { supportedFanModes += "fanOn " }
	if(cmd.circulation) { supportedFanModes += "fanCirculate " }

	state.supportedFanModes = supportedFanModes
    logging("${device} : supportedFanModes [${supportedFanModes}]", "info")
    sendEvent(name: "supportedFanModes", value: "[${supportedFanModes}]",descriptionText: "Driver ${state.version}", isStateChange:true)
// not getting any results back on this one
    
}

// these are untrapped log them...
def zwaveEvent(hubitat.zwave.commands.basicv1.BasicReport cmd) {
    logging("${device} : Received basicv1.BasicReport ${cmd}", "debug")
}
def zwaveEvent(hubitat.zwave.Command cmd) {
    logging("${device} : Received Command ${cmd}", "debug")
}

// Newer v5 format see what we get and log it.
def zwaveEvent(hubitat.zwave.commands.sensormultilevelv5.SensorMultilevelReport cmd){
    logging("${device} : Received Command ${cmd}", "debug")
}

void zwaveEvent(hubitat.zwave.commands.versionv2.VersionReport cmd) {
    logging("${device} : Received Command ${cmd}", "debug")
    device.updateDataValue("firmwareVersion", "${cmd.firmware0Version}.${cmd.firmware0SubVersion}")
    device.updateDataValue("protocolVersion", "${cmd.zWaveProtocolVersion}.${cmd.zWaveProtocolSubVersion}")
    device.updateDataValue("hardwareVersion", "${cmd.hardwareVersion}")
}




def quickSetHeat(degrees) {
	setHeatingSetpoint(degrees, 1000)
}

def setHeatingSetpoint(degrees, delay = 30000) {
	setHeatingSetpoint(degrees.toDouble(), delay)
}

def setHeatingSetpoint(Double degrees, Integer delay = 30000) {

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
    logging("${device} : Set Heat Setpoint --- ${convertedDegrees} ${locationScale} ---", "info")
    sendEvent(name: "SetHeat", value: convertedDegrees, unit:locationScale ,descriptionText: "Driver ${state.version}", isStateChange:true)
	delayBetween([
		zwave.thermostatSetpointV1.thermostatSetpointSet(setpointType: 1, scale: deviceScale, precision: p, scaledValue: convertedDegrees).format(),
		zwave.thermostatSetpointV1.thermostatSetpointGet(setpointType: 1).format()
	], delay)
}

def quickSetCool(degrees) {
	setCoolingSetpoint(degrees, 1000)
}

def setCoolingSetpoint(degrees, delay = 30000) {
	setCoolingSetpoint(degrees.toDouble(), delay)
}

def setCoolingSetpoint(Double degrees, Integer delay = 30000) {

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
    logging("${device} : Set Cool Setpoint --- ${convertedDegrees} ${locationScale} ---", "info")
    sendEvent(name: "SetCool", value: convertedDegrees, unit:locationScale ,descriptionText: "Driver ${state.version}", isStateChange:true)
	delayBetween([
		zwave.thermostatSetpointV1.thermostatSetpointSet(setpointType: 2, scale: deviceScale, precision: p,  scaledValue: convertedDegrees).format(),
		zwave.thermostatSetpointV1.thermostatSetpointGet(setpointType: 2).format()
	], delay)
}



def modes() {
	["off", "heat", "cool", "auto", "emergency heat"]
}

def switchMode() {
	def currentMode = device.currentState("thermostatMode")?.value
	def lastTriedMode = state.lastTriedMode ?: currentMode ?: "off"
	def supportedModes = getDataByName("supportedModes")
	def modeOrder = modes()
	def next = { modeOrder[modeOrder.indexOf(it) + 1] ?: modeOrder[0] }
	def nextMode = next(lastTriedMode)
	if (supportedModes?.contains(currentMode)) {
		while (!supportedModes.contains(nextMode) && nextMode != "off") {
			nextMode = next(nextMode)
		}
	}
	state.lastTriedMode = nextMode
    logging("${device} : Mode switch to ${nextMode}", "info")
	delayBetween([
		zwave.thermostatModeV2.thermostatModeSet(mode: modeMap[nextMode]).format(),
		zwave.thermostatModeV2.thermostatModeGet().format()
	], 1000)
}

def switchToMode(nextMode) {
    logging("${device} : Switch to Mode ${nextMode}", "info")
	def supportedModes = getDataByName("supportedModes")
	if(supportedModes && !supportedModes.contains(nextMode))  logging("${device} : not supported ${nextMode}", "warn")
	if (nextMode in modes()) {
		state.lastTriedMode = nextMode
		"$nextMode"()
	} else {
        logging("${device} : No Mode ${nextMode}", "warn")
	}
}

def switchFanMode() {
	def currentMode = device.currentState("thermostatFanMode")?.value
	def lastTriedMode = state.lastTriedFanMode ?: currentMode ?: "off"
	def supportedModes = getDataByName("supportedFanModes") ?: "fanAuto fanOn"
	def modeOrder = ["fanAuto", "fanCirculate", "fanOn"]
	def next = { modeOrder[modeOrder.indexOf(it) + 1] ?: modeOrder[0] }
	def nextMode = next(lastTriedMode)
	while (!supportedModes?.contains(nextMode) && nextMode != "fanAuto") {
		nextMode = next(nextMode)
	}
	switchToFanMode(nextMode)
}

def switchToFanMode(nextMode) {
	def supportedFanModes = getDataByName("supportedFanModes")
	if(supportedFanModes && !supportedFanModes.contains(nextMode)) log.warn "thermostat mode '$nextMode' is not supported"

	def returnCommand
	if (nextMode == "fanAuto") {
		returnCommand = fanAuto()
	} else if (nextMode == "fanOn") {
		returnCommand = fanOn()
	} else if (nextMode == "fanCirculate") {
		returnCommand = fanCirculate()
	} else {
		logging("${device} : No Fan Mode ${nextMode}", "warn")
	}
    logging("${device} : Fan Mode switch to ${nextMode}", "info")
	if(returnCommand) state.lastTriedFanMode = nextMode
	returnCommand
}

def getDataByName(String name) {
	state[name] ?: device.getDataValue(name)
}

def getModeMap() { [
	"off": 0,
	"heat": 1,
	"cool": 2,
	"auto": 3,
	"emergency heat": 4
]}

def setThermostatMode(String value) {
	delayBetween([
		zwave.thermostatModeV2.thermostatModeSet(mode: modeMap[value]).format(),
		zwave.thermostatModeV2.thermostatModeGet().format()
	], standardDelay)
}

def getFanModeMap() { [
	"auto": 0,
	"on": 1,
	"circulate": 6
]}

def setThermostatFanMode(String value) {
	delayBetween([
		zwave.thermostatFanModeV3.thermostatFanModeSet(fanMode: fanModeMap[value]).format(),
		zwave.thermostatFanModeV3.thermostatFanModeGet().format()
	], standardDelay)
}

def off() {
    logging("${device} : Set mode OFF", "info")
	delayBetween([
		zwave.thermostatModeV2.thermostatModeSet(mode: 0).format(),
		zwave.thermostatModeV2.thermostatModeGet().format()
	], standardDelay)
}

def heat() {
    logging("${device} : Set mode HEAT", "info")
	delayBetween([
		zwave.thermostatModeV2.thermostatModeSet(mode: 1).format(),
		zwave.thermostatModeV2.thermostatModeGet().format()
	], standardDelay)
}

def emergencyHeat() {
    logging("${device} : Set Mode Emergency heat", "info")
	delayBetween([
		zwave.thermostatModeV2.thermostatModeSet(mode: 4).format(),
		zwave.thermostatModeV2.thermostatModeGet().format()
	], standardDelay)
}

def cool() {
    logging("${device} : Set Mode Cool", "info")
	delayBetween([
		zwave.thermostatModeV2.thermostatModeSet(mode: 2).format(),
		zwave.thermostatModeV2.thermostatModeGet().format()
	], standardDelay)
}

def auto() {
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
        logging("${device} : ${encapsulatedCommand}", "debug")
        return zwaveEvent(encapsulatedCommand)
    }
}
def zwaveEvent(hubitat.zwave.commands.batteryv1.BatteryReport cmd) {
    def nowTime = new Date().time
    state.lastBatteryGet = nowTime
    def map = [ name: "battery", unit: "%" ]
    map.displayed = true
    map.isStateChange = true
    if (cmd.batteryLevel == 0xFF){
        cmd.batteryLevel = 0
    }
    map.value = cmd.batteryLevel
    if (cwire) { map.value =12 }// Forces battery report over low alarm
    if (map.value <= 10) {
        map.descriptionText = "battery is low!"
        logging("${device} : battery is low! ${cmd.batteryLevel} C wire:${cwire}", "info")
    } else {
        map.descriptionText = "battery is ok"
        logging("${device} : Battery ${cmd.batteryLevel} C wire:${cwire}", "info")

       sendEvent(name: map.name, value: map.value,unit: map.unit, descriptionText: "Driver ${state.version}", isStateChange:true)
    }
    map
}

private getBattery() {	
	def nowTime = new Date().time
	def ageInMinutes = state.lastBatteryGet ? (nowTime - state.lastBatteryGet)/60000 : 1440
//    log.debug "Battery report age: ${ageInMinutes} minutes"
    if (ageInMinutes <1440){ logging("${device} : Skipping Bat Fetch, Once a day only", "debug")}
    if (ageInMinutes >= 1440) {// 24 hrs
        state.lastBatteryGet = nowTime
        logging("${device} : Requesting Battery ", "info")
		zwave.batteryV1.batteryGet().format()
    } else "delay 87"
}
private ReSetClock(){
    state.lastClockSet = 1500
    setClock()
}



private setClock() {	
	def nowTime = new Date().time
	def ageInMinutes = state.lastClockSet ? (nowTime - state.lastClockSet)/60000 : 1440

    if (ageInMinutes < 1440){ // 24 hrs
        logging("${device} : Skipping Clock, Once a day only" ,"debug")
       } 
    
    if (ageInMinutes >= 1440) {
		state.lastClockSet = nowTime
        def nowCal = Calendar.getInstance(location.timeZone) // get current location timezone
        state.LastTimeSet = "${nowCal.getTime().format("EEE MMM dd yyyy HH:mm:ss z", location.timeZone)}"
        logging("${device} : Setting clock ${state.LastTimeSet}", "info")
        sendEvent(name: "SetClock", value: state.LastTimeSet, descriptionText: "Driver ${state.version}",displayed: true, isStateChange:true)
		zwave.clockV1.clockSet(hour: nowCal.get(Calendar.HOUR_OF_DAY), minute: nowCal.get(Calendar.MINUTE), weekday: nowCal.get(Calendar.DAY_OF_WEEK)).format()
    } else "delay 87"
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



