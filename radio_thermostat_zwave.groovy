/* Zwave Radio Thermostat Hubitat driver

Polling schedule pull current temp for 
thermostats that go to sleep for hrs.

May work on others but tested on 
CT-101 Iris version
CT-30e   
Polling only works on C wire mode.



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
    TheVersion="3.0"
 if (state.version != TheVersion){ 
     state.version = TheVersion
     configure() 
 }
}


metadata {

	definition (name: "Radio Thermostat Zwave", namespace: "tmastersmart", author: "tmaster", importUrl: "") {
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

//		command "switchMode"
//		command "switchFanMode"
        command "unschedule"
        command "ReSetClock"
//        command "quickSetCool"
//        command "quickSetHeat"

        

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
    input name: "polling", type: "number", title: "Polling", description: "Mins between poll", defaultValue: 15,required: true
}

def configure() {
    unschedule()
    logging("${device} : Configure Driver v${state.version}", "info")
	device.updateSetting("infoLogging",[value:"true",type:"bool"])
	device.updateSetting("debugLogging",[value:"true",type:"bool"])
	device.updateSetting("traceLogging",[value:"false",type:"bool"])
    
    // Poll the device every x min
	int checkEveryMinutes = polling			
	randomSixty = Math.abs(new Random().nextInt() % 60)
	schedule("${randomSixty} 0/${checkEveryMinutes} * * * ? *", poll)
    logging("${device} :Setting Chron for poll ${randomSixty} 0/${checkEveryMinutes} * * * ? *", "info")
	updated()
    delayBetween([
		zwave.thermostatModeV2.thermostatModeSupportedGet().format(),
	], 2300)
}



def updated() {
	loggingStatus()
	runIn(3600,debugLogOff)
	runIn(3500,traceLogOff)
	refresh()

}



def pollDevice() {
    logging("${device} :Polling", "info")
    poll()
}





def parse(String description)
{
   CommandClassCapabilities = [0x31:3,0x40:2,0x42:1,0x43:2,0x44:3,0x45:1,0x60:3,0x70:2,0x72:2,0x80:1,0x81:1,0x85:1,0x86:1,0x98:1,]   
   logging("${device} :Parseing ${description}", "trace")
//zw device: 27, command: 4003, payload: 02 , isMulticast: false
    

    
    hubitat.zwave.Command map = zwave.parse(description, CommandClassCapabilities)
    
//def map = hubitat.zwave.parse(description,CommandClassCapabilities ) //[0x42:1, 0x43:2, 0x31:3] 0x42:1 Operating State 0x40:2 Mode 0x31:3 Sensor

    
	if (!map) {
       logging("${device} : Map=NULL", "trace")
		return null
	}
//    def map = hubitat.zwave.parse(description, commandClassCapabilities)
	def result = [map]
    logging("${device} : Map=${map}", "trace")
    
    if (map) { 
        zwaveEvent(map)
        return
    }
    
	if ( map.name in ["heatingSetpoint","coolingSetpoint","thermostatMode"]) {
		def map2 = [
			name: "thermostatSetpoint",
			unit: getTemperatureScale()
		]
		if (map.name == "thermostatMode") {
			state.lastTriedMode = map.value
			if (map.value == "cool") {
				map2.value = device.latestValue("coolingSetpoint")
                logging("${device} : cooling setpoint = ${map2.value}", "info")
			}
			else {
				map2.value = device.latestValue("heatingSetpoint")
                logging("${device} : heating setpoint = ${map2.value}", "info")
			}
		}
		else {
			def mode = device.latestValue("thermostatMode")
            logging("${device} :mode = ${mode}", "info")
			if ((map.name == "heatingSetpoint" && mode == "heat") || (map.name == "coolingSetpoint" && mode == "cool")) {
				map2.value = map.value
				map2.unit = map.unit
			}
		}
		if (map2.value != null) {
            logging("${device} : adding setpoint event ${map}", "debug")
			result << createEvent(map2)
		}
	} else if (map.name == "thermostatFanMode" && map.isStateChange) {
		state.lastTriedFanMode = map.value
	}
    logging("${device} : Parse returned ${result}", "info")
	result
}

// Event Generation
def zwaveEvent(hubitat.zwave.commands.thermostatsetpointv2.ThermostatSetpointReport cmd)
{
    
    def cmdScale = cmd.scale == 1 ? "F" : "C"
	def map = [:]
	map.value = convertTemperatureIfNeeded(cmd.scaledValue, cmdScale, cmd.precision)
//    logging("${device} :Event Generate - ${map.value}", "debug")
	map.unit = getTemperatureScale()
	map.displayed = false
	switch (cmd.setpointType) {
		case 1:
			map.name = "heatingSetpoint"
        logging("${device} : ${map.name} ${map.value}", "info")
			break;
		case 2:
			map.name = "coolingSetpoint"
        logging("${device} : ${map.name} ${map.value}", "info")
			break;
		default:
        logging("${device} : ${map.name} ${map.value}", "debug")
			return [:]
	}
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
    logging("${device} : ${map.name} ${map.value}${map.unit}", "info")
	map
}

def zwaveEvent(hubitat.zwave.commands.thermostatoperatingstatev1.ThermostatOperatingStateReport cmd)
{
	def map = [:]
	switch (cmd.operatingState) {
		case hubitat.zwave.commands.thermostatoperatingstatev1.ThermostatOperatingStateReport.OPERATING_STATE_IDLE:
			map.value = "idle"
            logging("${device} : state- ${map.value}", "info")
			break
		case hubitat.zwave.commands.thermostatoperatingstatev1.ThermostatOperatingStateReport.OPERATING_STATE_HEATING:
			map.value = "heating"
            logging("${device} : state- ${map.value}", "info")
			break
		case hubitat.zwave.commands.thermostatoperatingstatev1.ThermostatOperatingStateReport.OPERATING_STATE_COOLING:
			map.value = "cooling"
         logging("${device} : state- ${map.value}", "info")
			break
		case hubitat.zwave.commands.thermostatoperatingstatev1.ThermostatOperatingStateReport.OPERATING_STATE_FAN_ONLY:
			map.value = "fan only"
         logging("${device} : state- ${map.value}", "info")
			break
		case hubitat.zwave.commands.thermostatoperatingstatev1.ThermostatOperatingStateReport.OPERATING_STATE_PENDING_HEAT:
			map.value = "pending heat"
         logging("${device} : state- ${map.value}", "info")
			break
		case hubitat.zwave.commands.thermostatoperatingstatev1.ThermostatOperatingStateReport.OPERATING_STATE_PENDING_COOL:
			map.value = "pending cool"
         logging("${device} : state- ${map.value}", "info")
			break
		case hubitat.zwave.commands.thermostatoperatingstatev1.ThermostatOperatingStateReport.OPERATING_STATE_VENT_ECONOMIZER:
			map.value = "vent economizer"
         logging("${device} : state- ${map.value}", "info")
			break
	}
	map.name = "thermostatOperatingState"
    logging("${device} : Rec - ${map.name} - ${map.value} ", "trace")
	map
}

def zwaveEvent(hubitat.zwave.commands.thermostatfanstatev1.ThermostatFanStateReport cmd) {
	def map = [name: "thermostatFanState", unit: ""]
	switch (cmd.fanOperatingState) {
		case 0:
			map.value = "idle"
            logging("${device} : Fan- ${map.value}", "info")
			break
		case 1:
			map.value = "running"
            logging("${device} : Fan- ${map.value}", "info")
			break
		case 2:
			map.value = "running high"
            logging("${device} : Fan- ${map.value}", "info")
			break
	}
    logging("${device} : Rec - ${map.name} - ${map.value} ", "trace")
	map
}

def zwaveEvent(hubitat.zwave.commands.thermostatmodev2.ThermostatModeReport cmd) {
	def map = [:]
	switch (cmd.mode) {
		case hubitat.zwave.commands.thermostatmodev2.ThermostatModeReport.MODE_OFF:
			map.value = "off"
         logging("${device} : mode- ${map.value}", "info")
			break
		case hubitat.zwave.commands.thermostatmodev2.ThermostatModeReport.MODE_HEAT:
			map.value = "heat"
         logging("${device} : mode- ${map.value}", "info")
			break
		case hubitat.zwave.commands.thermostatmodev2.ThermostatModeReport.MODE_AUXILIARY_HEAT:
			map.value = "emergency heat"
         logging("${device} : mode- ${map.value}", "info")
			break
		case hubitat.zwave.commands.thermostatmodev2.ThermostatModeReport.MODE_COOL:
			map.value = "cool"
         logging("${device} : mode- ${map.value}", "info")
			break
		case hubitat.zwave.commands.thermostatmodev2.ThermostatModeReport.MODE_AUTO:
			map.value = "auto"
         logging("${device} : mode- ${map.value}", "info")
			break
	}
	map.name = "thermostatMode"
    logging("${device} : Rec - ${map.name} - ${map.value} ", "trace")
    
	map
}

def zwaveEvent(hubitat.zwave.commands.thermostatfanmodev3.ThermostatFanModeReport cmd) {
	def map = [:]
	switch (cmd.fanMode) {
		case hubitat.zwave.commands.thermostatfanmodev3.ThermostatFanModeReport.FAN_MODE_AUTO_LOW:
			map.value = "fanAuto"
         logging("${device} : Fan- ${map.value}", "info")
			break
		case hubitat.zwave.commands.thermostatfanmodev3.ThermostatFanModeReport.FAN_MODE_LOW:
			map.value = "fanOn"
         logging("${device} : Fan- ${map.value}", "info")
			break
		case hubitat.zwave.commands.thermostatfanmodev3.ThermostatFanModeReport.FAN_MODE_CIRCULATION:
			map.value = "fanCirculate"
         logging("${device} : Fan- ${map.value}", "info")
			break
	}
	map.name = "thermostatFanMode"
    logging("${device} : Rec - ${map.name} - ${map.value} ", "trace")
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
    logging("${device} : supportedModes- ${supportedModes}", "debug")
}

def zwaveEvent(hubitat.zwave.commands.thermostatfanmodev3.ThermostatFanModeSupportedReport cmd) {
	def supportedFanModes = ""
	if(cmd.auto) { supportedFanModes += "fanAuto " }
	if(cmd.low) { supportedFanModes += "fanOn " }
	if(cmd.circulation) { supportedFanModes += "fanCirculate " }

	state.supportedFanModes = supportedFanModes
    logging("${device} : supportedFanModes- ${supportedFanModes}", "debug")
}

def zwaveEvent(hubitat.zwave.commands.basicv1.BasicReport cmd) {
    logging("${device} : received- ${cmd}", "debug")
}

def zwaveEvent(hubitat.zwave.Command cmd) {
    logging("${device} : received Unexpected- ${cmd}", "warn")
}

// Command Implementations
def poll() {
    clientVersion()
    logging("${device} : Poll Driver v${state.version}", "info")
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
    logging("${device} : Set Heat Setpoint ${convertedDegrees} ${locationScale}", "info")
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
    logging("${device} : Set Cool Setpoint ${convertedDegrees} ${locationScale}", "info")
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
    logging("${device} : Fan Mode ${nextMode}", "info")
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
    logging("${device} : Fan Mode ${nextMode}", "info")
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
    logging("${device} :multiinstancev1.MultiInstanceCmdEncap: command: ${cmd}", "trace")
    def encapsulatedCommand = cmd.encapsulatedCommand([0x31: 2])
    logging("${device} :multiinstancev1.MultiInstanceCmdEncap: command from instance ${cmd.instance}: ${encapsulatedCommand}", "trace")
    if (encapsulatedCommand) {
        return zwaveEvent(encapsulatedCommand)
    }
}
def zwaveEvent(hubitat.zwave.commands.batteryv1.BatteryReport cmd) {
    def nowTime = new Date().time
    state.lastBatteryGet = nowTime
    def map = [ name: "battery", unit: "%" ]
    map.displayed = true
    map.isStateChange = true
    if (cmd.batteryLevel == 0xFF || cmd.batteryLevel == 0) {
        map.value = 1
        map.descriptionText = "battery is low!"
        logging("${device} : battery is low!", "info")
    } else {
        map.value = cmd.batteryLevel
        logging("${device} : Battery ${cmd.batteryLevel}", "info")
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
// event not needed on clock set. Removed       
//        sendEvent(name: "SetClock", value: "setting clock to ${state.LastTimeSet}", displayed: true, isStateChange: true)
		zwave.clockV1.clockSet(hour: nowCal.get(Calendar.HOUR_OF_DAY), minute: nowCal.get(Calendar.MINUTE), weekday: nowCal.get(Calendar.DAY_OF_WEEK)).format()
    } else "delay 87"
}

def refresh() {
// Force a refresh
//  logging("${device} : refreshing", "info")
//   state.lastBatteryGet = (new Date().time) - (1440 * 60000) // whats this for?
//   state.lastClockSet = (new Date().time) - (1440 * 60000)
    clientVersion()
    poll()
}

void loggingStatus() {
	log.info "${device} : Logging : ${infoLogging == true}"
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



