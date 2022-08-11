/* Zwave Radio Thermostat Hubitat driver
Hubitat driver for radio thermostat & Iris Thermostat
Radio Thermostat Company of America (RTC)

Supports
poll chron, time set chron,humidity,heat or cool only,C-wire,Diff,Recovery mode,Mans detection
setpoint restore from state memory. 
______          _ _         _____ _                                   _        _   
| ___ \        | (_)       |_   _| |                                 | |      | |  
| |_/ /__ _  __| |_  ___     | | | |__   ___ _ __ _ __ ___   ___  ___| |_ __ _| |_ 
|    // _` |/ _` | |/ _ \    | | | '_ \ / _ \ '__| '_ ` _ \ / _ \/ __| __/ _` | __|
| |\ \ (_| | (_| | | (_) |   | | | | | |  __/ |  | | | | | | (_) \__ \ || (_| | |_ 
\_| \_\__,_|\__,_|_|\___/    \_/ |_| |_|\___|_|  |_| |_| |_|\___/|___/\__\__,_|\__|
                                                                                  

Auto Correct can reset a thermostats setpoint from the last one sent. Correcting missed setpoint commans
or restoring from a local manual change.

Polling chron ends the need to use rouines to refreash the thermostat as some models just dont report
temp and setpoints to the hub unless polled.
Versions of the same model act diffrently depending on firmware. Some ct101 report some dont. 

Heating Colling only supports using the thermostat only for heaters  or ac units. Prevents the blocked mode
commans and removes blocked modes from the options in scripts.

Driver was written to fix the many problems with internal drivers not supporting fully the ct101
and the ct30 thermostats. The thermostats have bugs that need special programming.

Tested on.... 
USNAP Module RTZW-01 n
fingerprint mfr:0098 prod:6501 model:000C ct101 Iris version
fingerprint mfr:0098 prod:1E12 model:015C ct30e rev v1 C-wire report    
fingerprint mfr:0098 prod:0001 model:001E ct30e  Displays REMOTE CONTROL box when paired - No c-wire report  

If you have version that identifies as UNKNOWN please send me your fingerprint and the version on the thermostat. 
If your version has a version # that doesnt match the fingerprints bellow please send me your fingerprint and version.


ZWAVE SPECIFIC_TYPE_THERMOSTAT_GENERAL_V2
===================================================================================================
 v5.2.7 08/11/2022 Bug fixes. to many to list
 v5.2   08/07/2022 mode bug fixed
 v5.1   08/05/2022 Changes to manufacturerSpecificGet. Heat or Cool Only working
 v4.8   08/04/2022 First major release out of beta working code.
 v3.3   07/30/2022 Improvements and debuging code.      
 v3.1   07/30/2022 Total rewrite of event storage and Parsing.
 v3.0   07/29/2022 Release - Major fixes and log conversion
 -----             Missing numbers are beta working local versions never released.
===================================================================================================
Notes


Paring. Bring hub to thermostat or thermostat to hub connect 24 volts AC between C and RC. (AC ONLY). 
Be sure you get paired in c-wire mode.

CT101 will work in battery mode or c-wire mode. However I havent done much testing in battery mode.

Models with the Radio Thermostat Z-Wave USNAP Module RTZW-01 Require C-Wire or they will go to sleep
never to rewake. Some will take commands in sleep some wont. c-wire is required.
Do not even atempt to use battery mode. 

Some models report C-Wire status some dont. 

There are many diffrent versions of the ct30 some with bad firmware and some that dont support all commands.
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





May use some open source code (Apache License, Version 2.0) from many drivers. 

Orginal driver:
https://github.com/SmartThingsCommunity/SmartThingsPublic/blob/master/devicetypes/smartthings/ct100-thermostat.src/ct100-thermostat.groovy

These all apear to be forked from the orginal above.
https://github.com/MarioHudds/hubitat/blob/master/Enhanced%20Z-Wave%20Thermostat
https://community.hubitat.com/t/port-enhanced-z-wave-thermostat-ct-100-w-humidity-and-time-update/4743
https://github.com/motley74/SmartThingsPublic/blob/master/devicetypes/motley74/ct100-thermostat-custom.src/ct100-thermostat-custom.groovy
*/

def clientVersion() {
    TheVersion="5.2.7"
 if (state.version != TheVersion){ 
     state.version = TheVersion

     configure() // Forces config on updates
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
        attribute "SetClock", "string"
        attribute "SetCool", "string"
        attribute "SetHeat", "string"
        
//		command "switchMode"
//		command "switchFanMode"
        command "unschedule"
        command "uninstall"
        command "setClock"
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
       fingerprint type:"0806", mfr:"0098", prod:"0002", model:"0100",manufacturer: "Radio Thermostat", deviceJoinName:"CT-32 Z-Wave Thermostat" 
       fingerprint type:"0806", mfr:"0098", prod:"2002", model:"0102",manufacturer: "Radio Thermostat", deviceJoinName:"CT-32 Z-Wave Thermostat" 
    
       fingerprint type:"0806", mfr:"0098", prod:"3200", model:"015E",manufacturer: "Radio Thermostat", deviceJoinName:"CT-50 Filtrete 3M-50 Thermostat" 
       fingerprint type:"0806", mfr:"0098", prod:"5003", model:"0109",manufacturer: "Radio Thermostat", deviceJoinName:"CT-80 Radio Thermostat"
       fingerprint type:"0806", mfr:"0098", prod:"5003", model:"01FD",manufacturer: "Radio Thermostat", deviceJoinName:"CT-80 Radio Thermostat" 
       fingerprint type:"0806", mfr:"0098", prod:"6401", model:"0107",manufacturer: "Radio Thermostat", deviceJoinName:"CT-100 Radio Thermostat"
       fingerprint type:"0806", mfr:"0098", prod:"6401", model:"0106",manufacturer: "Radio Thermostat", deviceJoinName:"CT-100 Vivint Thermostat"
       fingerprint type:"0806", mfr:"0098", prod:"6402", model:"0100",manufacturer: "Radio Thermostat", deviceJoinName:"CT-100 Plus Radio Thermostat"
       fingerprint type:"0806", mfr:"0098", prod:"6501", model:"00FD",manufacturer: "Radio Thermostat", deviceJoinName:"CT-101 Radio Thermostat"
       fingerprint type:"0806", mfr:"0098", prod:"6501", model:"000B",manufacturer: "Radio Thermostat", deviceJoinName:"CT-101 Lowes Thermostat"
       fingerprint type:"0806", mfr:"0098", prod:"6501", model:"000C",manufacturer: "Radio Thermostat", deviceJoinName:"CT-101 Iris Thermostat" 
        
       fingerprint type:"0806", mfr:"0098", prod:"C801", model:"001D",manufacturer: "Radio Thermostat", deviceJoinName:"CT-200 Vivint Element Thermostat" 
       fingerprint type:"0806", mfr:"0098", prod:"C801", model:"0022",manufacturer: "Radio Thermostat", deviceJoinName:"CT-200X Vivint Element Thermostat" 

// 6402:0100       
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
//    input name: "autocorrect", type: "bool", title: "Auto Correct setpoints", description: "Keep thermostat settings matching hub (this will overide local changes)", defaultValue: false,required: true
//    input(  "autocorrectNum", "number", title: "Auto Correct errors", description: "send auto corect after number of errors detected. ", defaultValue: 3,required: true)
    input(  "polling", "number", title: "Polling", description: "Mins between poll. Must config after changing", defaultValue: 15,required: true)

}

def installed(){
logging("${device} : Radio Thermostat Paired!", "info")
cleanState()    
configure()
}

void cleanState(){
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
    state.remove("error")
    
        // no longer used
    state.remove("lastClockSet") 
    state.remove("supportedFanModes")
    state.remove("supportedModes")

removeDataValue("thermostatSetpoint")    
removeDataValue("SetCool")
removeDataValue("coolingSetpoint")    
removeDataValue("SetHeat")   

// Clear crap from other drivers 
updateDataValue("hardwareVersion", "")    
updateDataValue("protocolVersion", "")
updateDataValue("lastRunningMode", "")
updateDataValue("zwNNUR", "")

    logging("${device} : Garbage Collection.", "info")    
   
}

def uninstall() {
	unschedule()
    cleanState()
    logging("${device} : Uninstalled", "info")   
}

def configure() {
    unschedule()
    state.icon = "<img src='data:image/jpeg;base64,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'>"
    logging("${device} : Configure Driver v${state.version}", "info")
	device.updateSetting("infoLogging",[value:"true",type:"bool"])
	device.updateSetting("debugLogging",[value:"false",type:"bool"])
	device.updateSetting("traceLogging",[value:"false",type:"bool"])
	updated()
    state.cwire =0 
//    state.error = 0
    delayBetween([
        zwave.manufacturerSpecificV2.manufacturerSpecificGet().format(),// fingerprint
		zwave.thermostatModeV2.thermostatModeSupportedGet().format(),
        zwave.thermostatFanModeV3.thermostatFanModeSupportedGet().format(), 
        zwave.configurationV2.configurationSet(parameterNumber: 4, size: 1, configurationValue: [2]).format(), // cwire enabled
        zwave.configurationV2.configurationGet(parameterNumber: 4).format(), // is cwire 1=true 2=false
        zwave.configurationV2.configurationGet(parameterNumber: 9).format(), // is fast recovery on ? 1on 0 off
        zwave.configurationV2.configurationGet(parameterNumber: 8).format(), // is diff
        getBattery(), 
//        setClock(), 
	], 2300)
}



def updated() {
    // Poll the device every x min
    clientVersion()
    if (polling <10) {polling=15}
    if (polling >59) {polling=45}
    
	int checkEveryMinutes = polling	
	randomSixty = Math.abs(new Random().nextInt() % 60)
	schedule("${randomSixty} 0/${checkEveryMinutes} * * * ? *", poll)
    schedule("${randomSixty} 0 12 * * ? *", setTheClock)
    logging("${device} :Setting Chron Poll:${randomSixty} 0/${checkEveryMinutes} * * * ? *  Clock:${randomSixty} 0 12 * * ? *  ", "info")

    loggingStatus()
	runIn(3600,debugLogOff)
	runIn(3500,traceLogOff)
    
    delayBetween([
    zwave.thermostatModeV2.thermostatModeGet().format(),// get mode
    zwave.sensorMultilevelV3.sensorMultilevelGet().format() // current temperature 
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
    
    def nowCal = Calendar.getInstance(location.timeZone)
    Timecheck = "${nowCal.getTime().format("EEE MMM dd", location.timeZone)}"
    logging("${device} : Poll E=Event# ${Timecheck} v${state.version}", "info")
//zwave.sensorMultilevelV5.sensorMultilevelGet().format(), // testing
//zwave.batteryV1.batteryGet().format(),
//zwave.commands.versionv2.VersionReport
//zwave.thermostatModeV2.thermostatModeSupportedGet().format(),
//zwave.manufacturerSpecificV2.manufacturerSpecificGet().format(),
	delayBetween([
		zwave.sensorMultilevelV3.sensorMultilevelGet().format(),// current temperature
        zwave.multiInstanceV1.multiInstanceCmdEncap(instance: 2).encapsulate(zwave.sensorMultilevelV2.sensorMultilevelGet()).format(), // CT-100/101 Humidity
		zwave.thermostatSetpointV1.thermostatSetpointGet(setpointType: 1).format(),
		zwave.thermostatSetpointV1.thermostatSetpointGet(setpointType: 2).format(),
		zwave.thermostatModeV2.thermostatModeGet().format(),
		zwave.thermostatFanModeV3.thermostatFanModeGet().format(),
		zwave.thermostatOperatingStateV1.thermostatOperatingStateGet().format(),
        zwave.configurationV2.configurationGet(parameterNumber: 9).format(),// is fast recovery
        zwave.configurationV2.configurationGet(parameterNumber: 8).format(),// is temp diff
        zwave.configurationV2.configurationGet(parameterNumber: 4).format(),// get cwire
		getBattery(), 
//        setClock(), // moved to chron
	], 2300)
}
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
   //def zwcmd = zwave.parse(description, [0x42:2, 0x43:2, 0x31: 2, 0x60: 3]) old code
   CommandClassCapabilities = [0x31:3,0x40:2,0x42:1,0x43:2,0x44:3,0x45:1,0x60:3,0x70:2,0x72:2,0x80:1,0x81:1,0x85:1,0x86:1]   
   hubitat.zwave.Command map = zwave.parse(description, CommandClassCapabilities)
    logging("${device} : Raw [${description}]", "trace")
    if (map == null) {return null}
	def result = [map]
    if (!result) {return null}
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
    sendEvent(name: map.name , value: map.value,unit: cmdScale,descriptionText:"${map.name} ${map.value} ${state.version}", isStateChange:true)    
    }
    tempCheck2 = map.value.toDouble() 
    if(tempCheck){ // needed in case of no last setpoints set
     if(tempCheck == tempCheck2){
      logging("${device} : E1 ${map.name} Set Points Match Last${tempCheck}=Current${tempCheck2}", "info")
//      state.error = 0 
      }
     if(tempCheck != tempCheck2){
         logging("${device} : E1 ${map.name} Last Point does not match Last${tempCheck}<>Current${tempCheck2} ", "warn") 
// This doesnt work wont take set cmd from here? Why.
//         state.error = state.error +1
//         if (state.error >= autocorrectNum) {
//           if (autocorrect == true){ // Set in config. optional
//                 setCoolingSetpoint(state.SetCool) doesnt work
//                 setHeatingSetpoint(state.SetHeat) doesnt work
         
//        logging("${device} : E1 Resetting cool:${state.SetCool}/Heat:${state.SetHeat}", "info")
//        logging("${device} : E1 (setpointType: 1, scale: ${state.scale}, precision: ${state.precision}, scaledValue: ${state.SetHeat})", "info")
//        logging("${device} : E1 (setpointType: 2, scale: ${state.scale}, precision: ${state.precision}, scaledValue: ${state.SetCool})", "info")        
               
//        state.error = 0 eben sending cmd from here doesnt work..
 //       delayBetween([
//        zwave.thermostatSetpointV1.thermostatSetpointSet(setpointType: 1, scale: state.scale, precision: state.precision, scaledValue: state.SetHeat) ,   
//        zwave.thermostatSetpointV1.thermostatSetpointGet(setpointType: 1),
//        zwave.thermostatSetpointV1.thermostatSetpointSet(setpointType: 2, scale: state.scale, precision: state.precision, scaledValue: state.SetCool),
//        zwave.thermostatSetpointV1.thermostatSetpointGet(setpointType: 2)    
//	], delay)   
//           }    
//         }   
      }
   }// end if not null

// stored so we can use on send
	state.size = cmd.size
	state.scale = cmd.scale
	state.precision = cmd.precision
}
// E2 
def zwaveEvent(hubitat.zwave.commands.sensormultilevelv2.SensorMultilevelReport cmd)
{
	logging("${device} : received E2 ${cmd}", "debug")
    def map = [:]
    map.displayed = true
    map.isStateChange = true
    if (cmd.sensorType == 0) {return }  // ct30 (fixed in rev1)
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
    sendEvent(name:map.name ,value:map.value ,unit: map.unit, descriptionText:"${map.name} ${map.value} ${state.version}", isStateChange: true)
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
    sendEvent(name: map.name, value: map.value,descriptionText: "${map.name} ${map.value} ${state.version}", isStateChange:true)
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
    sendEvent(name: map.name, value: map.value,descriptionText: "${map.name} ${map.value} ${state.version}", isStateChange:true)
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
    sendEvent(name: map.name, value: map.value,descriptionText: "${map.name} ${map.value} ${state.version}", isStateChange:true)
    
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
    sendEvent(name: map.name, value: map.value,descriptionText: "${map.name} ${map.value} ${state.version}", isStateChange:true)
 
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
        

//	state.supportedModes = supportedModes
    logging("${device} : E7 supportedModes [${supportedModes}]", "info")
    sendEvent(name: "supportedThermostatModes", value: "[${supportedModes}]",descriptionText: "${supportedModes} ${state.version}", isStateChange:true)

  
//    supportedThermostatModes : [off, heat, cool, auto, emergency heat]
    
}
// E8
def zwaveEvent(hubitat.zwave.commands.thermostatfanmodev3.ThermostatFanModeSupportedReport cmd) {
	def supportedFanModes = ""
    logging("${device} : received E8 ${cmd}", "debug")
	if(cmd.auto) { supportedFanModes += "fanAuto," }
	if(cmd.low) { supportedFanModes += "fanOn," }
	if(cmd.circulation) { supportedFanModes += "fanCirculate, " } // not used
//  if(cmd.humidityCirculation)supportedFanModes += "fanHumCirculate, " } // not used
//	if(cmd.high) { supportedFanModes += "fanHigh," } // not used
    logging("${device} : E8 supportedFanModes[${supportedFanModes}]", "info")
    sendEvent(name: "supportedFanModes", value: "[${supportedModes}]",descriptionText: "${supportedModes} ${state.version}", isStateChange:true)

    
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
            sendEvent(name: "powerSource", value: "mains",descriptionText: "Power Mains ${state.version}", isStateChange: true)
        }
        if (state.cwire == 2){
            logging("${device} : E10 C-Wire :FALSE PowerSouce :battery", "info") 
            sendEvent(name: "powerSource", value: "battery",descriptionText: "Power Battery ${state.version}", isStateChange: true)
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
    
    if (map.type=="2002" ){ state.model ="CT32"}// 0002:0100,2002:0100,2002:0102 
    if (map.type=="0002" ){ state.model ="CT32"}
    if (map.type=="3200" ){ state.model ="CT50"}
    if (map.type=="5003" ){ state.model ="CT80"}
    if (map.type=="6401" | map.type=="6402"){ state.model ="CT100"} 
    if (map.type=="6402" | map.type=="0100"){ state.model ="CT100 Plus"}
    if (map.type=="6501"){   
      state.model ="CT101"
      if (map.model=="000B") {state.model ="CT101 iris"}
   }
    if (map.type=="C801"){
        state.model = "CT200"
        if(map.model =="001D"){state.model ="CT200 Vivant"}
        if(map.model =="0022"){state.model = "CT200x Vivant"}
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


    
// have yet to see data here
def zwaveEvent(hubitat.zwave.commands.versionv2.VersionReport cmd) {
    logging("${device} : Received E12 ${cmd}", "info")
    device.updateDataValue("firmwareVersion", "${cmd.firmware0Version}.${cmd.firmware0SubVersion}")
    device.updateDataValue("protocolVersion", "${cmd.zWaveProtocolVersion}.${cmd.zWaveProtocolSubVersion}")
    device.updateDataValue("hardwareVersion", "${cmd.hardwareVersion}")
}

//==================heating

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
    sendEvent(name: "SetHeat", value: convertedDegrees, unit:locationScale ,descriptionText: "Reset Last to ${state.SetHeat} ${state.version}", isStateChange:true)
    sendEvent(name: "thermostatSetpoint", value: convertedDegrees, unit:locationScale ,descriptionText: "Reset Last to ${state.SetHeat} ${state.version}", isStateChange:true)
    
     
    
    logging("${device} : Set (heat type=1) scale:${deviceScale}, precision:${p},  scaledValue:${convertedDegrees}", "trace")
	delayBetween([
		zwave.thermostatSetpointV1.thermostatSetpointSet(setpointType: 1, scale: deviceScale, precision: p, scaledValue: convertedDegrees).format(),
		zwave.thermostatSetpointV1.thermostatSetpointGet(setpointType: 1).format()
	], delay)
}

//==================cooling

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
    sendEvent(name: "SetCool", value: convertedDegrees, unit:locationScale ,descriptionText: "Reset Last to ${state.SetCool} ${state.version}", isStateChange:true)
    sendEvent(name: "thermostatSetpoint", value: convertedDegrees, unit:locationScale ,descriptionText: "Reset Last to ${state.SetCool} ${state.version}", isStateChange:true)

    
    
    logging("${device} :Set (cool type=2) scale:${deviceScale}, precision:${p},  scaledValue:${convertedDegrees}", "trace")

    
	delayBetween([
		zwave.thermostatSetpointV1.thermostatSetpointSet(setpointType: 2, scale: deviceScale, precision: p, scaledValue: convertedDegrees).format(),
		zwave.thermostatSetpointV1.thermostatSetpointGet(setpointType: 2).format()
	], delay)
 
}



def getDataByName(String name) {
	state[name] ?: device.getDataValue(name)
}

// E20 receives Hub mode command
def setThermostatMode(String value) {
    logging("${device} : E20 setThermostatMode  ${value}", "trace")
    if(!value){return}
    if(value == "off"){ set= 0}
    if(value == "heat"){
        set =1
        if(onlyMode == "coolonly"){
       coolOnly()
       return
     } 
    }
    if(value == "cool"){
        set =2
        if(onlyMode == "heatonly"){
       heatingOnly()
       return
     }
    }
    
    if(value == "auto"){
         if(onlyMode == "heatonly" | onlyMode =="coolonly"){
         noAuto()    
         return
         }
        set =3
    }
    if(value == "emergency heat"){
        set =4
            if(onlyMode == "coolonly"){
       coolOnly()
       return
     } 
    }
 
    logging("${device} : E20 Set Mode:${value} #:${set}", "info")
	delayBetween([
		zwave.thermostatModeV2.thermostatModeSet(mode: set).format(),
		zwave.thermostatModeV2.thermostatModeGet().format()
	], standardDelay)
}
// E21
def setThermostatFanMode(String value) {
    logging("${device} : E21 setThermostatFanMode   ${value}", "trace")
    if (!value){return}
    if(value == "auto"){     set=0}
    if(value == "on"){       set=1}
    if(value == "circulate"){set=2}
    
    logging("${device} : E21 Set Fan Mode:${value} #:${set}", "info")
	delayBetween([
		zwave.thermostatFanModeV3.thermostatFanModeSet(fanMode: set).format(),
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

void noAuto(){
    logging("${device} : When in ${onlyMode} auto disabled", "info") 
}

// off = 0
def off() {
    logging("${device} : Set mode OFF", "info")
	delayBetween([
		zwave.thermostatModeV2.thermostatModeSet(mode: 0).format(),
		zwave.thermostatModeV2.thermostatModeGet().format()
	], standardDelay)
}
// 1
def heat() {
        //   "onlyMode" ["off", "heatonly","coolonly"]     
    if(onlyMode == "coolonly"){
       coolOnly()
       return
   }
    logging("${device} : Set Mode heat", "info")
	delayBetween([
		zwave.thermostatModeV2.thermostatModeSet(mode: 1).format(),
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
     if(onlyMode == "heatonly" | onlyMode =="coolonly"){
         noAuto()
         return 
     }
    logging("${device} : Set Mode Auto", "info")
	delayBetween([
		zwave.thermostatModeV2.thermostatModeSet(mode: 3).format(),
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
        sendEvent(name: "powerSource", value: "mains",descriptionText: "Power Mains ${state.version}", isStateChange: true)
        return
    }

    map.value = cmd.batteryLevel
    test = cmd.batteryLevel
    if (state.cwire == 1){extra="Mains power"}
    if (state.cwire == 2){extra="Battery power"}
    if (state.cwire == 0){extra="Unknown power"} 
                    
    logging("${device} : E15 battery ${test}% ${extra}", "info")
    sendEvent(name: map.name, value: test,unit: map.unit, descriptionText: "${test}% ${extra} ${state.version}", isStateChange:true)

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

def setTheClock(){
//logging("${device} : Chron Seting the clock", "debug")   
setClock()
}
// Auto set clock code (improved)
// Day is not visiable in ct101 but is on ct30
private setClock() {
//	def ageInMinutes = state.lastClockSet ? (nowTime - state.lastClockSet)/60000 : 1440
//    if (ageInMinutes >= 60) { // once a hr
//		state.lastClockSet = nowTime
        def nowTime = new Date().time
        def nowCal = Calendar.getInstance(location.timeZone) // get current location timezone
        state.LastTimeSet = "${nowCal.getTime().format("EEE MMM dd HH:mm z", location.timeZone)}"
        weekday = "${nowCal.getTime().format("EEE", location.timeZone)}" // gives weekday name
//        weekdayNo = "${nowCal.getTime().format("V", location.timeZone)}" // gives wekday 1 = mon
 
//      setDay(nowCal.get(Calendar.DAY_OF_WEEK)) // gives us weekday name
        theTime ="${weekday} ${nowCal.get(Calendar.HOUR_OF_DAY)}:${nowCal.get(Calendar.MINUTE)}"
        weekdayZ = nowCal.get(Calendar.DAY_OF_WEEK) -1 // Gives us zwave weekday code (-1)
        if (weekdayZ <1){weekdayZ = 7} // rotate to up 7=sunday 
    logging("${device} : Adjusting clock (${theTime}) ${state.LastTimeSet}", "info")
        sendEvent(name: "SetClock", value: theTime, descriptionText: "${theTime} ${state.version}",displayed: true, isStateChange:true)

        delayBetween([
		zwave.clockV1.clockSet(hour: nowCal.get(Calendar.HOUR_OF_DAY), minute: nowCal.get(Calendar.MINUTE), weekday: weekdayZ).format(),
        zwave.clockV1.clockGet().format()
	], standardDelay)
}

void setDay(day){
    // This is the Zwave format day
    // The zwave day is 1 less than the hub
    if (day==1){weekday="Mon"}
    if (day==2){weekday="Tue"}
    if (day==3){weekday="Wed"}
    if (day==4){weekday="Thu"}
    if (day==5){weekday="Fri"}
    if (day==6){weekday="Sat"}
    if (day==7){weekday="Sun"}  
}

def zwaveEvent(hubitat.zwave.commands.clockv1.ClockReport cmd) {
    setDay(cmd.weekday)
    def nowCal = Calendar.getInstance(location.timeZone) // get current location timezone
    Timecheck = "${nowCal.getTime().format("EEE MMM dd yyyy HH:mm:ss z", location.timeZone)}"
    daycheck =  "${nowCal.getTime().format("EEE", location.timeZone)}"
    setclock= false 
   
    if (weekday != daycheck){ 
       setclock=true
       error = "${weekday} <> ${daycheck }"
    }
    if (cmd.hour    != nowCal.get(Calendar.HOUR_OF_DAY)){
        setclock=true
         error = "${cmd.hour} <> ${nowCal.get(Calendar.HOUR_OF_DAY)} "
    }
    if (cmd.minute  != nowCal.get(Calendar.MINUTE)){     
        setclock=true
         error = "${cmd.minute} <> ${nowCal.get(Calendar.MINUTE)} "
    }
    if (setclock == false) {logging("${device} : E16 Rec   clock (${weekday} ${cmd.hour}:${cmd.minute}) ok", "info")}
    if (setclock == true) {logging("${device} : E16 Rec   clock ${weekday} ${cmd.hour}:${cmd.minute}) (out of sync) ${error}", "warn")}
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
