/* Radio Thermostat Zwave Hubitat driver
Hubitat driver for radio thermostat & Iris Thermostat
Radio Thermostat Company of America (RTC)
CT100,CT101,CT30,CT32

Supports
poll chron, time set chron,humidity,heat or cool only,C-wire,Diff,Mans detection

______          _ _         _____ _                                   _        _   
| ___ \        | (_)       |_   _| |                                 | |      | |  
| |_/ /__ _  __| |_  ___     | | | |__   ___ _ __ _ __ ___   ___  ___| |_ __ _| |_ 
|    // _` |/ _` | |/ _ \    | | | '_ \ / _ \ '__| '_ ` _ \ / _ \/ __| __/ _` | __|
| |\ \ (_| | (_| | | (_) |   | | | | | |  __/ |  | | | | | | (_) \__ \ || (_| | |_ 
\_| \_\__,_|\__,_|_|\___/    \_/ |_| |_|\___|_|  |_| |_| |_|\___/|___/\__\__,_|\__|
                                                                                  

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
 v5.5.2 11/12/2022 Logging module update. Autofix installed
 v5.5.1 10/20/2022 Null on line 401. Error checking added/ Swing and DIff auto saved
 v5.5.0 10/15/2022 Null detection added in event 11
 v5.4.2 10/10/2022 Split info logs 
 v5.4.1 09/28/2022 Loging for chron human form
 v5.4   09/19/2022 Rewrote logging routines.
 v5.3.5 08/17/2022 Bug fix Last update broke the clock fixed
 v5.3.4 08/16/2022 Added Recovery mode
 v5.3.1 08/15/2022 Added Swing
 v5.3   08/14/2022 Added 2 stage differential
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




Contains a lot of orginal code and

May use some open source code from sources listed here. 
https://raw.githubusercontent.com/tmastersmart/hubitat-code/main/opensource_links.txt

Orginal driver:
https://github.com/SmartThingsCommunity/SmartThingsPublic/blob/master/devicetypes/smartthings/ct100-thermostat.src/ct100-thermostat.groovy

These all apear to be forked from the orginal above.
https://github.com/MarioHudds/hubitat/blob/master/Enhanced%20Z-Wave%20Thermostat
https://community.hubitat.com/t/port-enhanced-z-wave-thermostat-ct-100-w-humidity-and-time-update/4743
https://github.com/motley74/SmartThingsPublic/blob/master/devicetypes/motley74/ct100-thermostat-custom.src/ct100-thermostat-custom.groovy
*/

def clientVersion() {
    TheVersion="5.5.2"
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
        
        command "setDiff"
        command "setSwing"
        command "unschedule"
        command "uninstall"
        command "setClock"
        command "setRecovery"
//        command "saveSettings"
  
		fingerprint deviceId: "0x08"
		fingerprint inClusters: "0x43,0x40,0x44,0x31"
        
        

        
        fingerprint inClusters: "0x20,0x87,0x72,0x31,0x40,0x42,0x44,0x43,0x86"//                     * ct-30e Z-Wave USNAP Module RTZW-01
        fingerprint inClusters: "0x20,0x87,0x72,0x31,0x40,0x44,0x43,0x42,0x86,0x70,0x80,0x88" //     * ct-30 private lable alarm
        fingerprint inClusters: "0x20,0x81,0x87,0x72,0x31,0x40,0x44,0x43,0x42,0x86,0x70,0x80,0x88" //* ct-30e rev v1 Z-Wave USNAP Module RTZW-01
        
        fingerprint inClusters: "0x20,0x81,0x87,0x72,0x31,0x40,0x42,0x44,0x45,0x43,0x86,0x70,0x80,0x85,0x60" //      ct-100 & (ct-101 iris)
        fingerprint inClusters: "0x20,0x81,0x87,0x72,0x31,0x40,0x42,0x44,0x45,0x43,0x86,0x70,0x80,0x85,0x5D,0x60"//  ct-101
// hubitat ignores deviceJoinName only included for ref notes        
//  https://www.opensmarthouse.org/zwavedatabase/94
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
        //  https://www.opensmarthouse.org/zwavedatabase/98
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
    
    input name: "infoLogging",  type: "bool", title: "Enable info logging", description: "Recomended info level" ,defaultValue: true,required: true
    input name: "info2Logging", type: "bool", title: "Enable info Extra logging", description: "Recomended info2 level" ,defaultValue: true,required: true
	input name: "debugLogging", type: "bool", title: "Enable debug logging", description: "Programming Debug logs" ,defaultValue: false,required: true
	input name: "traceLogging", type: "bool", title: "Enable trace logging", description: "Insane HIGH level", defaultValue: false,required: true

    input(  "heatDiff", "number", title: "Heat differential 2 Stage", description: "When does 2nd stage engage. 4=cold areas 8=warm areas.", defaultValue: 4,required: true)
    input(  "coolDiff", "number", title: "Cool differential 2 Stage", description: "Cool differential. Only for 2 stage Heatpumps.", defaultValue: 4,required: true)
    input(  "swing", "enum", title: "Temperature Swing", description: "Number of degrees above (for cooling) and below (for heating) the temp will fluctuate before cycling back on.", options: ["0.5","1.0","1.5","2.0","2.5","3.0","3.5","4.0"], defaultValue: "1.0", multiple: false, required: true)

    
    input name: "recovery", type: "enum", title: "Recovery mode", description: "Fast or economy. ",  options: ["fast", "economy"], defaultValue: "economy",required: true 
   
    input name: "onlyMode", type: "enum", title: "Mode Bypass", description: "Heat or Cool only mode",  options: ["off", "heatonly","coolonly"], defaultValue: "off",required: true 
    input(  "polling", "enum", title: "Polling minutes", description: "Polling Chron. Press Config after changing ", options: ["10","15","20","30","40","50"],defaultValue: 15,required: true)

    
    input name: "autocorrect", type: "bool", title: "Auto Correct setpoints", description: "Keep thermostat settings matching hub (this will overide local changes)", defaultValue: false,required: true
    input(  "autocorrectNum", "number", title: "Auto Correct errors", description: "send auto corect after number of errors detected. ", defaultValue: 5,required: true)


}

def installed(){
logging("Radio Thermostat Paired!", "info")
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
    state.remove("donate") 
       
    
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
    state.remove("lastBatteryGet")

removeDataValue("thermostatSetpoint")    
removeDataValue("SetCool")
removeDataValue("coolingSetpoint")    
removeDataValue("SetHeat")   

// Clear crap from other drivers 
updateDataValue("hardwareVersion", "")    
updateDataValue("protocolVersion", "")
updateDataValue("lastRunningMode", "")
updateDataValue("zwNNUR", "")

    logging("Garbage Collection.", "info")    
   
}

def uninstall() {
	unschedule()
    cleanState()
    logging("Uninstalled", "info")   
}

def configure() {
    unschedule()
    state.remove("paypal") 
    getIcons()
    
    state.info ="All thermostats do not work with all settings. Only settings reported in the log as being received will work."
    logging("Configure Driver v${state.version}", "info")

	updated()
    state.cwire =0 
    state.remove("lastBatteryGet")
    delayBetween([
        zwave.manufacturerSpecificV2.manufacturerSpecificGet().format(),// fingerprint
		zwave.thermostatModeV2.thermostatModeSupportedGet().format(),
        zwave.thermostatFanModeV3.thermostatFanModeSupportedGet().format(), 
        zwave.configurationV2.configurationSet(parameterNumber: 4, size: 1, configurationValue: [2]).format(), // cwire enabled
        zwave.configurationV2.configurationGet(parameterNumber: 4).format(), // is cwire 1=true 2=false
        zwave.configurationV2.configurationGet(parameterNumber: 7).format(), // swing        
        zwave.configurationV2.configurationGet(parameterNumber: 8).format(), // is diff
        zwave.configurationV2.configurationGet(parameterNumber: 9).format(), // is fast recovery on ? 1on 0 off        
        zwave.batteryV1.batteryGet().format(), 
//        setClock(), 
	], 2300)
}



def updated() {
    // Poll the device every x min
    clientVersion()
    if (!polling){polling=15}
    // options: ["10","15","20","30","40","50"]
    int checkEveryMinutes = Integer.parseInt(polling)
	randomSixty = Math.abs(new Random().nextInt() % 60)
	schedule("${randomSixty} 0/${checkEveryMinutes} * * * ? *", poll)
    schedule("${randomSixty} 0 12 * * ? *", setTheClock)
    logging("Setting Chron Poll: every ${checkEveryMinutes}mins  Clock: 12:${randomSixty}", "info")

    loggingUpdate()
    
    delayBetween([
    zwave.thermostatModeV2.thermostatModeGet().format(),// get mode
    zwave.sensorMultilevelV3.sensorMultilevelGet().format(), // current temperature 
    saveSettings()
    ], 2300)    

    
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
    logging("Poll E=Event# ${Timecheck} v${state.version}", "info")
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
        zwave.configurationV2.configurationGet(parameterNumber: 4).format(),// get cwire
        zwave.configurationV2.configurationGet(parameterNumber: 7).format(),// temp swing
        zwave.configurationV2.configurationGet(parameterNumber: 8).format(),// is temp diff
        zwave.configurationV2.configurationGet(parameterNumber: 9).format(),// is fast recovery
		zwave.batteryV1.batteryGet().format(),
//        setClock(), // moved to chron
	], 2300)
}
//    zwave.configurationV2.configurationSet(parameterNumber: 11, size: 1, configurationValue: 1) // simple UI enabled 1 on 2 off
//    zwave.configurationV2.configurationGet(parameterNumber: 11) 


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
   if (!map) {
   logging("Unable to Parse", "error")   
       return
   }
	def result = [map]
    logging("Raw:${description}", "trace")
    logging("Parse ${map}", "debug")
    zwaveEvent(map)
}

// Event Generation
// event E1
// Termostat setpoints -------------------------------------------------------
def zwaveEvent(hubitat.zwave.commands.thermostatsetpointv2.ThermostatSetpointReport cmd)
{
    logging("received E1 ${cmd}", "debug")
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
        logging("error Unknown ${map.value}", "warn")
			return [:]
	}
    if (map.name != null){

    logging("E1 ${map.name} ${map.value} ${cmdScale}", "info")
    sendEvent(name: map.name , value: map.value,unit: cmdScale,descriptionText:"${map.name} ${map.value} ${state.version}", isStateChange:true)    
    }
    tempCheck2 = map.value.toDouble() 
    if(tempCheck){ // needed in case of no last setpoints set
     if(tempCheck == tempCheck2){
      logging("E1 ${map.name} Set Points Match Last${tempCheck}=Current${tempCheck2}", "debug") //moved to debug
      state.error = 0 
      }
     if(tempCheck != tempCheck2 & autocorrect == true){
         logging("E1 ${map.name} Last Point does not match Last${tempCheck}<>Current${tempCheck2} error:${state.error} fixOn:${autocorrectNum}", "warn")
         state.error = state.error + 1
         if (state.error >= autocorrectNum ){
          runIn(1,FixHeat)
          runIn(5,FixCool)
          logging("Resetting cool:${state.SetCool}/Heat:${state.SetHeat}", "info")
          //state.error = 0    
         }    
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
	logging("received E2 ${cmd}", "debug")
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
    logging("E2 ${map.name} ${map.value} ${map.unit}", "info")
    sendEvent(name:map.name ,value:map.value ,unit: map.unit, descriptionText:"${map.name} ${map.value} ${state.version}", isStateChange: true)
    }
    else {logging("E2 Unknown data ${cmd}", "warn")}
	map
}
// E3
def zwaveEvent(hubitat.zwave.commands.thermostatoperatingstatev1.ThermostatOperatingStateReport cmd)
{
	def map = [:]
    logging("received E3 ${cmd}", "debug")
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
	
    logging("E3 ${map.name} - ${map.value} ", "info2")
    sendEvent(name: map.name, value: map.value,descriptionText: "${map.name} ${map.value} ${state.version}", isStateChange:true)
	map
}
// E4
def zwaveEvent(hubitat.zwave.commands.thermostatfanstatev1.ThermostatFanStateReport cmd) {
    logging("received E4 ${cmd}", "debug")
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
    logging("received E4 ${map.name} - ${map.value} ", "info2")
    sendEvent(name: map.name, value: map.value,descriptionText: "${map.name} ${map.value} ${state.version}", isStateChange:true)
	map
}

def zwaveEvent(hubitat.zwave.commands.thermostatmodev2.ThermostatModeReport cmd) {
	def map = [:]
    logging("received E5 ${cmd}", "debug")
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
	
    logging("E5 ${map.name} - ${map.value} ", "info")
    sendEvent(name: map.name, value: map.value,descriptionText: "${map.name} ${map.value} ${state.version}", isStateChange:true)
    
	map
}

def zwaveEvent(hubitat.zwave.commands.thermostatfanmodev3.ThermostatFanModeReport cmd) {
	def map = [:]
    logging("received E6 ${cmd}", "debug")
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
	
    logging("E6 ${map.name} - ${map.value} ", "info2")
    sendEvent(name: map.name, value: map.value,descriptionText: "${map.name} ${map.value} ${state.version}", isStateChange:true)
 
	map.displayed = false
	map
}

def zwaveEvent(hubitat.zwave.commands.thermostatmodev2.ThermostatModeSupportedReport cmd) {
	def supportedModes = ""
    logging("received E7 ${cmd}", "debug")
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
    logging("E7 supportedModes [${supportedModes}]", "info2")
    sendEvent(name: "supportedThermostatModes", value: "[${supportedModes}]",descriptionText: "${supportedModes} ${state.version}", isStateChange:true)

  
//    supportedThermostatModes : [off, heat, cool, auto, emergency heat]
    
}
// E8
def zwaveEvent(hubitat.zwave.commands.thermostatfanmodev3.ThermostatFanModeSupportedReport cmd) {
	def supportedFanModes = ""
    logging("received E8 ${cmd}", "debug")
	if(cmd.auto) { supportedFanModes += "fanAuto," }
	if(cmd.low) { supportedFanModes += "fanOn," }
	if(cmd.circulation) { supportedFanModes += "fanCirculate, " } // not used
//  if(cmd.humidityCirculation)supportedFanModes += "fanHumCirculate, " } // not used
//	if(cmd.high) { supportedFanModes += "fanHigh," } // not used
    logging("E8 supportedFanModes[${supportedFanModes}]", "info2")
    sendEvent(name: "supportedFanModes", value: "[${supportedModes}]",descriptionText: "${supportedModes} ${state.version}", isStateChange:true)

    
}
// these are untrapped log them...
def zwaveEvent(hubitat.zwave.commands.basicv1.BasicReport cmd) {
    logging("Received E9 ${cmd}", "debug")
}

def zwaveEvent(hubitat.zwave.commands.configurationv2.ConfigurationReport cmd) {
    logging("Received E10 ${cmd}", "debug")
//  ConfigurationReport(parameterNumber: 4, size: 1, configurationValue: [1], scaledConfigurationValue: 1)
    if (cmd.parameterNumber== 4){
//      state.cwire = cmd.configurationValue[0]
        state.cwire = cmd.scaledConfigurationValue
        if (state.cwire == 1){
            logging("E10-4 C-Wire :TRUE PowerSouce :mains", "info2")
            sendEvent(name: "powerSource", value: "mains",descriptionText: "Power Mains ${state.version}", isStateChange: true)
        }
        if (state.cwire == 2){
            logging("E10-4 C-Wire :FALSE PowerSouce :battery", "info2") 
            sendEvent(name: "powerSource", value: "battery",descriptionText: "Power Battery ${state.version}", isStateChange: true)
        }
    }    
//  ConfigurationReport(parameterNumber: 7, size: 1, configurationValue: 
    if (cmd.parameterNumber== 7){
    def test = cmd.scaledConfigurationValue  
    def value = 2
    def locationScale = getTemperatureScale()    
    if (test == 1){value = "0.5"}
    if (test == 2){value = "1.0"}
    if (test == 3){value = "1.5"}
    if (test == 4){value = "2.0"}
    if (test == 5){value = "2.5"}
    if (test == 6){value = "3.0"}
    if (test == 7){value = "3.5"}
    if (test == 8){value = "4.0"}    
    state.swing = value    
    logging("E10-7 Temp Swing :${state.swing} ${locationScale} - #${test}", "info2")
    sendEvent(name: "temperatureSwing", value: state.swing, descriptionText: "${state.swing}${locationScale} - #${test} ${state.version}",displayed: true, isStateChange:true)
    }    
    
// ConfigurationReport(parameterNumber: 8, size: 2, configurationValue: [4, 4], scaledConfigurationValue: 1028) Diff
   if (cmd.parameterNumber== 8){
       state.heatDiff = cmd.configurationValue[0] 
       state.coolDiff = cmd.configurationValue[1] 
       logging("E10-8 2 stage Differential Heat:${state.heatDiff} Cool:${state.coolDiff}", "info2")
   }
// ConfigurationReport(parameterNumber: 9, size: 1, configurationValue: [2], scaledConfigurationValue: 2)  Fast Recovery
   if (cmd.parameterNumber== 9){
       if (cmd.configurationValue[0] == 1){state.fastrecovery = "fast"}
       if (cmd.configurationValue[0] == 2){state.fastrecovery = "economy"} 
       logging(" E10-9 Recovery :${state.fastrecovery} #${cmd.configurationValue[0]}", "info2")
   }    
   
   
}

//No such property: manufacturerId for class: hubitat.zwave.commands.thermostatsetpointv2.ThermostatSetpointSupportedReport on line 717 (method parse)
def zwaveEvent(hubitat.zwave.Command cmd ){
  logging("Received E11 (${cmd})", "debug")
  if(cmd == null |cmd.manufacturerId == null ) {
      logging("Received E11 NULL", "warn")
      return
  }  
 
	def map = [:]
    map.name = "ManufacturerSpecificReport"
    map.mfr   = hubitat.helper.HexUtils.integerToHexString(cmd.manufacturerId, 2)
    map.model = hubitat.helper.HexUtils.integerToHexString(cmd.productId, 2)
    map.type  = hubitat.helper.HexUtils.integerToHexString(cmd.productTypeId, 2)
    logging("E11 fingerprint mfr:${map.mfr} prod:${map.type} model:${map.model}", "debug")
   
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
     
      
    
    logging("E11 fingerprint ${state.model} [${map.mfr}-${map.type}-${map.model}] ", "info")
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
    logging("Received E12 ${cmd}", "info")
    device.updateDataValue("firmwareVersion", "${cmd.firmware0Version}.${cmd.firmware0SubVersion}")
    device.updateDataValue("protocolVersion", "${cmd.zWaveProtocolVersion}.${cmd.zWaveProtocolSubVersion}")
    device.updateDataValue("hardwareVersion", "${cmd.hardwareVersion}")
}

//==================heating
def FixHeat(){
//state.SetCool
 logging("Recovery HEATing Setpoint", "warn")   
 setCoolingSetpoint(state.SetHeat)
}


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
    logging("Set Heat Setpoint ${convertedDegrees} ${locationScale} ---  Reset Last to ${state.SetHeat}", "info")
    sendEvent(name: "SetHeat", value: convertedDegrees, unit:locationScale ,descriptionText: "Reset Last to ${state.SetHeat} ${state.version}", isStateChange:true)
    sendEvent(name: "thermostatSetpoint", value: convertedDegrees, unit:locationScale ,descriptionText: "Reset Last to ${state.SetHeat} ${state.version}", isStateChange:true)
    
     
    
    logging("Set (heat type=1) scale:${deviceScale}, precision:${p},  scaledValue:${convertedDegrees}", "trace")
	delayBetween([
		zwave.thermostatSetpointV1.thermostatSetpointSet(setpointType: 1, scale: deviceScale, precision: p, scaledValue: convertedDegrees).format(),
        zwave.thermostatSetpointV1.thermostatSetpointSet(setpointType: 1, scale: deviceScale, precision: p, scaledValue: convertedDegrees).format(),
		zwave.thermostatSetpointV1.thermostatSetpointGet(setpointType: 1).format()
	], 30000)
}

//==================cooling

def FixCool(){
//state.SetCool
 logging("Recovery COOLing Setpoint", "warn")   
 setCoolingSetpoint(state.SetCool)
}

def setCoolingSetpoint(degrees, delay = 30000) {
    logging("Set Cool Setpoint ${degrees} delay:${delay}", "debug")
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
    logging("Set Cool Setpoint ${convertedDegrees} ${locationScale} ---Reset Last to ${state.SetCool}", "info")
    sendEvent(name: "SetCool", value: convertedDegrees, unit:locationScale ,descriptionText: "Reset Last to ${state.SetCool} ${state.version}", isStateChange:true)
    sendEvent(name: "thermostatSetpoint", value: convertedDegrees, unit:locationScale ,descriptionText: "Reset Last to ${state.SetCool} ${state.version}", isStateChange:true)

    
    
    logging("Set (cool type=2) scale:${deviceScale}, precision:${p},  scaledValue:${convertedDegrees}", "trace")

    
	delayBetween([
		zwave.thermostatSetpointV1.thermostatSetpointSet(setpointType: 2, scale: deviceScale, precision: p, scaledValue: convertedDegrees).format(),
		zwave.thermostatSetpointV1.thermostatSetpointSet(setpointType: 2, scale: deviceScale, precision: p, scaledValue: convertedDegrees).format(),
        zwave.thermostatSetpointV1.thermostatSetpointGet(setpointType: 2).format()
	], 30000)
 
}



def getDataByName(String name) {
	state[name] ?: device.getDataValue(name)
}

// E20 receives Hub mode command
def setThermostatMode(String value) {
    logging("E20 setThermostatMode  ${value}", "trace")
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
 
    logging("E20 Set Mode:${value} #:${set}", "info")
	delayBetween([
		zwave.thermostatModeV2.thermostatModeSet(mode: set).format(),
		zwave.thermostatModeV2.thermostatModeGet().format()
	], 30000)
}
// E21
def setThermostatFanMode(String value) {
    logging("E21 setThermostatFanMode   ${value}", "trace")
    if (!value){return}
    if(value == "auto"){     set=0}
    if(value == "on"){       set=1}
    if(value == "circulate"){set=2}
    
    logging("E21 Set Fan Mode:${value} #:${set}", "info")
	delayBetween([
		zwave.thermostatFanModeV3.thermostatFanModeSet(fanMode: set).format(),
		zwave.thermostatFanModeV3.thermostatFanModeGet().format()
	], 30000)   
    
}

// -------------------------------------mode setting ------------------
//   "onlyMode" ["off", "heatonly","coolonly"] 
void coolOnly(){
logging("Cooling Only Heat disabled", "info") 
state.setHeat = ""    
removeDataValue("heatingSetpoint")    
removeDataValue("SetHeat")
  state.remove("setHeat")    
}
void heatingOnly(){
logging("Heating Only Cool disabled", "info") 
state.setCool = ""    
removeDataValue("coolingSetpoint")    
removeDataValue("SetCool")
  state.remove("setCool")    
}

void noAuto(){
    logging("When in ${onlyMode} auto disabled", "info") 
}

// off = 0
def off() {
    logging("Set mode OFF", "info")
	delayBetween([
		zwave.thermostatModeV2.thermostatModeSet(mode: 0).format(),
		zwave.thermostatModeV2.thermostatModeGet().format()
	], 30000)
}
// 1
def heat() {
        //   "onlyMode" ["off", "heatonly","coolonly"]     
    if(onlyMode == "coolonly"){
       coolOnly()
       return
   }
    logging("Set Mode heat", "info")
	delayBetween([
		zwave.thermostatModeV2.thermostatModeSet(mode: 1).format(),
		zwave.thermostatModeV2.thermostatModeGet().format()
	], 30000)
}

def cool() {
//   "onlyMode" ["off", "heatonly","coolonly"]     
    if(onlyMode == "heatonly"){
       heatingOnly()
       return
    } 

    
    logging("Set Mode Cool", "info")
	delayBetween([
		zwave.thermostatModeV2.thermostatModeSet(mode: 2).format(),
		zwave.thermostatModeV2.thermostatModeGet().format()
	], 30000)
}

def auto() {
//   "onlyMode" ["off", "heatonly","coolonly"]     
     if(onlyMode == "heatonly" | onlyMode =="coolonly"){
         noAuto()
         return 
     }
    logging("Set Mode Auto", "info")
	delayBetween([
		zwave.thermostatModeV2.thermostatModeSet(mode: 3).format(),
		zwave.thermostatModeV2.thermostatModeGet().format()
	], 30000)
}

def emergencyHeat() {
    //   "onlyMode" ["off", "heatonly","coolonly"]     
    if(onlyMode == "coolonly"){
       coolOnly()
       return
    } 
    logging("Set Mode Emergency heat", "info")
	delayBetween([
		zwave.thermostatModeV2.thermostatModeSet(mode: 4).format(),
		zwave.thermostatModeV2.thermostatModeGet().format()
	], 30000)
}





def fanOn() {
    logging("Set Fan ON", "info")
	delayBetween([
		zwave.thermostatFanModeV3.thermostatFanModeSet(fanMode: 1).format(),
		zwave.thermostatFanModeV3.thermostatFanModeGet().format()
	], 30000)
}

def fanAuto() {
    logging("Set Fan Auto", "info")
	delayBetween([
		zwave.thermostatFanModeV3.thermostatFanModeSet(fanMode: 0).format(),
		zwave.thermostatFanModeV3.thermostatFanModeGet().format()
	], 30000)
}

def fanCirculate() {
    logging("Set Fan Circulate", "info")
	delayBetween([
		zwave.thermostatFanModeV3.thermostatFanModeSet(fanMode: 6).format(),
		zwave.thermostatFanModeV3.thermostatFanModeGet().format()
	], 30000)
}

//private getStandardDelay() {
//	1000
//}

// CUSTOMIZATIONS
def zwaveEvent(hubitat.zwave.commands.multiinstancev1.MultiInstanceCmdEncap cmd) {   
//    Decapsulate command 
    def encapsulatedCommand = cmd.encapsulatedCommand([0x31: 2])
    if (encapsulatedCommand) {
        logging("E14 ${encapsulatedCommand}", "debug")
        return zwaveEvent(encapsulatedCommand)
    }
}
// E15
def zwaveEvent(hubitat.zwave.commands.batteryv1.BatteryReport cmd) {
    logging("E15 battery  ${cmd}", "debug")
    if (cmd.batteryLevel == 0xFF){ // I have never seen this but its in the spec.
        logging("---- Power Restored ----", "info")
        sendEvent(name: "powerSource", value: "mains",descriptionText: "Power Mains ${state.version}", isStateChange: true)
        return
    }
                
    logging("E15 battery ${cmd.batteryLevel}% ", "info")
    sendEvent(name: "battery", value: cmd.batteryLevel ,unit: "%", descriptionText: "${cmd.batteryLevel}% ${state.version}", isStateChange:true)
}

private getBattery() {	
        logging("Requesting Battery", "debug")
		zwave.batteryV1.batteryGet().format()
  
}

def setTheClock(){
//logging("${device} : Chron Seting the clock", "debug")   
setClock()
}
// Auto set clock code (improved)
// Day is not visiable in ct101 but is on ct30
private setClock(cmd) {
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
    logging("Adjusting clock (${theTime}) ${state.LastTimeSet}", "info")
        sendEvent(name: "SetClock", value: theTime, descriptionText: "${theTime} ${state.version}",displayed: true, isStateChange:true)

        delayBetween([
		zwave.clockV1.clockSet(hour: nowCal.get(Calendar.HOUR_OF_DAY), minute: nowCal.get(Calendar.MINUTE), weekday: weekdayZ).format(),
        zwave.clockV1.clockGet().format(),
        zwave.batteryV1.batteryGet().format()    
	], 30000)
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
    if (setclock == false) {logging("E16 Rec   clock (${weekday} ${cmd.hour}:${cmd.minute}) ok", "info")}
    if (setclock == true) { logging("E16 Rec   clock ${weekday} ${cmd.hour}:${cmd.minute}) (out of sync) ${error}", "warn")}
}

def saveSettings(cmd){
 logging("SaveSettings", "debug")  
 delayBetween([
     setDiff(cmd),
     setSwing(cmd),
     setRecovery(cmd),
     setClock()
 ], 30000)     
   
}


def setDiff(cmd){
// 2 stage differential
    
   coolDiff = (coolDiff as Integer) 
   heatDiff = (heatDiff as Integer) 
    
   if (!coolDiff){coolDiff = 4}
   if (!heatDiff){heatDiff = 4}  
   logging("Set 2 stage Differential Heat:${heatDiff} Cool:${coolDiff}", "info")

    delayBetween([    
   zwave.configurationV2.configurationSet(parameterNumber: 8, size: 2, configurationValue: [0x00, heatDiff]).format(),
   zwave.configurationV2.configurationSet(parameterNumber: 8, size: 2, configurationValue: [0x01, coolDiff]).format(), 
   zwave.configurationV2.configurationGet(parameterNumber: 8).format(),    
	], 30000)    
}

def setSwing(cmd){
    
// options: ["0.5","1.0","1.5","2.0","2.5","3.0","3.5","4.0"]
    def value = 2
    def locationScale = getTemperatureScale()
    if (swing == "0.5"){value = 1}
    if (swing == "1.0"){value = 2}
    if (swing == "1.5"){value = 3}
    if (swing == "2.0"){value = 4}
    if (swing == "2.5"){value = 5}
    if (swing == "3.0"){value = 6}
    if (swing == "3.5"){value = 7}
    if (swing == "4.0"){value = 8}

    logging("Set Temp Swing:${swing} ${locationScale}", "info")

    
   delayBetween([    
   zwave.configurationV2.configurationSet(parameterNumber: 7, size: 1, configurationValue: [value]).format(),
   zwave.configurationV2.configurationGet(parameterNumber: 7).format(),    
	], 30000)  

}

def setRecovery(cmd){
// ConfigurationReport(parameterNumber: 9, size: 1, configurationValue: [2], scaledConfigurationValue: 2)  Fast Recovery
    if(!recovery){recovery = 2}
 
    if (recovery == "fast"){value = 1}
    if (recovery == "economy"){value = 2}
   
    logging("Set Recovery to:${recovery} ", "info")
    
    
   delayBetween([    
   zwave.configurationV2.configurationSet(parameterNumber: 9, size: 1, configurationValue: [value]).format(),
   zwave.configurationV2.configurationGet(parameterNumber: 9).format(),    
	], 30000)  

}


void getIcons(){
    state.icon ="<img src='https://github.com/tmastersmart/hubitat-code/raw/main/images/radio-thermostat.jpg'>"
    state.donate="<a href='https://www.paypal.com/paypalme/tmastersat?locale.x=en_US'><img src='https://raw.githubusercontent.com/tmastersmart/hubitat-code/main/images/paypal2.gif'></a>"
 
 }


// Logging block  v4.1
// 4 mode logging mod
void loggingUpdate() {
    logging("Logging Info:[${infoLogging}] Debug:[${debugLogging}] Trace:[${traceLogging}]", "infoBypass")
    // Only do this when its needed
    if (debugLogging){
        logging("Debug log:off in 3000s", "warn")
        runIn(3000,debugLogOff)
    }
    if (traceLogging){
        logging("Trace log: off in 1800s", "warn")
        runIn(1800,traceLogOff)
    }
}

void traceLogOff(){
	device.updateSetting("traceLogging",[value:"false",type:"bool"])
	log.trace "${device} : Trace Logging : Automatically Disabled"
}
void debugLogOff(){
	device.updateSetting("debugLogging",[value:"false",type:"bool"])
	log.debug "${device} : Debug Logging : Automatically Disabled"
}
private logging(String message, String level) {
    if (level == "infoBypass"){log.info  "${device} : $message"}
	if (level == "error"){     log.error "${device} : $message"}
	if (level == "warn") {     log.warn  "${device} : $message"}
	if (level == "trace" && traceLogging) {log.trace "${device} : $message"}
	if (level == "debug" && debugLogging) {log.debug "${device} : $message"}
    if (level == "info"  && infoLogging)  {log.info  "${device} : $message"}
    if (level == "info2" && info2Logging) {log.info  "${device} :* $message"}
}

