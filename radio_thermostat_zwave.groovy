/* Radio Thermostat Zwave Hubitat driver
Hubitat driver for radio thermostat
Hubitat driver for Iris CT101 Thermostat
Radio Thermostat Company of America (RTC)

CT30,CT32,CT50,CT80,CT100,CT101,CT110,CT200

Supports
poll chron, time set chron,humidity,heat or cool only,C-wire,Diff,Mans detection

______          _ _         _____ _                                   _        _   
| ___ \        | (_)       |_   _| |                                 | |      | |  
| |_/ /__ _  __| |_  ___     | | | |__   ___ _ __ _ __ ___   ___  ___| |_ __ _| |_ 
|    // _` |/ _` | |/ _ \    | | | '_ \ / _ \ '__| '_ ` _ \ / _ \/ __| __/ _` | __|
| |\ \ (_| | (_| | | (_) |   | | | | | |  __/ |  | | | | | | (_) \__ \ || (_| | |_ 
\_| \_\__,_|\__,_|_|\___/    \_/ |_| |_|\___|_|  |_| |_| |_|\___/|___/\__\__,_|\__|
                                                                                  
No battery is needed on these thrmostats if using C Wire. The driver will restore settings on reboot.
Unneeded batteries cost money so you can stop using them. Set driver to ignore battery

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

If you have version that identifies as UNKNOWN please send me your fingerprint and the version on the thermostat. 
If your version has a version # that doesnt match the fingerprints bellow please send me your fingerprint and version.
If any of the settings dont work on your thermostat please let me know.

CT30e [0098-0001-001E] USNAP Module RTZW-01 (tested) protocolVersion: 2.78 applicationVersion: 5.0
Has defective firmware
Displays REMOTE CONTROL box when paired.
False operating states. Claims its running all the time 
never reports idle unless in OFF mode. Causes dashboard color to stay red or blue.
Doesnt support modes other CT30s do. Wont reply to bat request.
May report energySaveHeat:true, energySaveCool: true Then report False.


ZWAVE SPECIFIC_TYPE_THERMOSTAT_GENERAL_V2
===================================================================================================
 v5.7.0 02/16/2023 minor cleanup/logging changes. CT110 fingerprint added. 2 Stage Diff reporting fixed
                   New testing for supported functions. Non supported functions now hidden and never sent.
 v5.6.9 02/11/2023 Fan Operating State now simulated when not sent from therm. Minor changes to improve reporting
 v5.6.8 02/10/2023 Total Rewrite of MODE code and tracing logging, Last update broke things, fixed
 v5.6.7 02/07/2023 Extra trace logging for sent commands
 v5.6.6 02/06/2023 BUG FIX setpoints now wait for therm to report back before setting. Delays changed
 v5.6.5 02/05/2023 2-stage diff reports not working, Config changes,Dashboard improvements
 v5.6.4 12/26/2022 Bug fix supportedFanModes. Thermostat modes fix
 v5.6.0 12/26/2022 Upgrade thermostat modes with double quotes to comply with new firmware 2.3.4.123 change
 v5.5.6 12/14/2022 Ignore bat option for mains only. No bat is actualy needed
 v5.5.5 12/05/2022 ManufacturerSpecificReport parsing added
 v5.5.4 11/23/2022 Bug fix in recovery error counter getting reset/Bug in heat reset was resetting cool
 v5.5.3 11/18/2022 extra polling times added
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

Some models report C-Wire status some dont. Some models report Diff some take the command but report back 6 

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




Contains a lot of orginal code created by me
and some code from these drivers

None of these drivers work right on radio Thermostat they all have problems.
Orginal smartthings driver:
https://github.com/SmartThingsCommunity/SmartThingsPublic/blob/master/devicetypes/smartthings/ct100-thermostat.src/ct100-thermostat.groovy
These are all forks from the above.
https://github.com/MarioHudds/hubitat/blob/master/Enhanced%20Z-Wave%20Thermostat
https://community.hubitat.com/t/port-enhanced-z-wave-thermostat-ct-100-w-humidity-and-time-update/4743
https://github.com/motley74/SmartThingsPublic/blob/master/devicetypes/motley74/ct100-thermostat-custom.src/ct100-thermostat-custom.groovy

May use some open source code from sources listed here. 
https://raw.githubusercontent.com/tmastersmart/hubitat-code/main/opensource_links.txt
*/

def clientVersion() {
    TheVersion="5.7.0"
 if (state.version != TheVersion){ 
     state.version = TheVersion
     runIn(10,configure)// Forces config on updates
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
        capability "HealthCheck"
        
        attribute "thermostatFanState", "string"
        attribute "SetClock", "string"
        attribute "SetCool", "string"
        attribute "SetHeat", "string"
        attribute "temperatureSwing", "string"
        attribute "recovery", "string"
        attribute "recoveryTempDiffHeat", "string"
        attribute "recoveryTempDiffCool", "string"
        
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
       fingerprint type:"0806", mfr:"0098", prod:"0001", model:"001E",manufacturer: "Radio Thermostat", deviceJoinName:"CT-30e Radio Thermostat" //https://products.z-wavealliance.org/products/158?selectedFrequencyId=2
       fingerprint type:"0806", mfr:"0098", prod:"0001", model:"0000",manufacturer: "Radio Thermostat", deviceJoinName:"CT-30 Z-Wave Thermostat" 
       fingerprint type:"0806", mfr:"0098", prod:"0001", model:"00FF",manufacturer: "Radio Thermostat", deviceJoinName:"CT-30 Z-Wave Thermostat" 
       fingerprint type:"0806", mfr:"0098", prod:"0001", model:"00FF",manufacturer: "Radio Thermostat", deviceJoinName:"CT-30 Z-Wave Thermostat"
       fingerprint type:"0806", mfr:"0098", prod:"2002", model:"0100",manufacturer: "Radio Thermostat", deviceJoinName:"CT-32 Z-Wave Thermostat" //https://products.z-wavealliance.org/products/1330?selectedFrequencyId=2 https://products.z-wavealliance.org/products/1046?selectedFrequencyId=2
       fingerprint type:"0806", mfr:"0098", prod:"0002", model:"0100",manufacturer: "Radio Thermostat", deviceJoinName:"CT-32 Z-Wave Thermostat" 
       fingerprint type:"0806", mfr:"0098", prod:"2002", model:"0102",manufacturer: "Radio Thermostat", deviceJoinName:"CT-32 Z-Wave Thermostat" 
    
       fingerprint type:"0806", mfr:"0098", prod:"3200", model:"015E",manufacturer: "Radio Thermostat", deviceJoinName:"CT-50 Filtrete 3M-50 Thermostat" 
       fingerprint type:"0806", mfr:"0098", prod:"5003", model:"0109",manufacturer: "Radio Thermostat", deviceJoinName:"CT-80 Radio Thermostat"
       fingerprint type:"0806", mfr:"0098", prod:"5003", model:"01FD",manufacturer: "Radio Thermostat", deviceJoinName:"CT-80 Radio Thermostat" 

        // There are 4 diffrent versions 8.4,8.7,9,9.1
       fingerprint type:"0806", mfr:"0098", prod:"6401", model:"0015",manufacturer: "Radio Thermostat", deviceJoinName:"CT-100 v8.7 Radio Thermostat"// https://products.z-wavealliance.org/products/646?selectedFrequencyId=2
       fingerprint type:"0806", mfr:"0098", prod:"6401", model:"0107",manufacturer: "Radio Thermostat", deviceJoinName:"CT-100 Radio Thermostat"//also 2gig
       fingerprint type:"0806", mfr:"0098", prod:"6401", model:"0106",manufacturer: "Radio Thermostat", deviceJoinName:"CT-100 v9.1 Vivint Thermostat"// https://products.z-wavealliance.org/products/795?selectedFrequencyId=2
       fingerprint type:"0806", mfr:"0098", prod:"6402", model:"0100",manufacturer: "Radio Thermostat", deviceJoinName:"CT-100 Plus Radio Thermostat"//https://products.z-wavealliance.org/products/1798?selectedFrequencyId=2

        
        //  https://www.opensmarthouse.org/zwavedatabase/98
       fingerprint type:"0806", mfr:"0098", prod:"6501", model:"00FD",manufacturer: "Radio Thermostat", deviceJoinName:"CT-101 Radio Thermostat"//https://products.z-wavealliance.org/products/1301?selectedFrequencyId=2
       fingerprint type:"0806", mfr:"0098", prod:"6501", model:"000B",manufacturer: "Radio Thermostat", deviceJoinName:"CT-101 Lowes Thermostat"
       fingerprint type:"0806", mfr:"0098", prod:"6501", model:"000C",manufacturer: "Radio Thermostat", deviceJoinName:"CT-101 Iris Thermostat" 
        
       fingerprint type:"0806", mfr:"0098", prod:"6E01", model:"0000",manufacturer: "Radio Thermostat", deviceJoinName:"CT-110 Thermostat" //https://products.z-wavealliance.org/products/1333
         

        
        
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
    updateParameters()
    input name: "infoLogging",  type: "bool", title: "Enable info logging", description: "Recomended info level" ,defaultValue: true,required: true
    input name: "info2Logging", type: "bool", title: "Enable info Extra logging", description: "Recomended info2 level. Full info level." ,defaultValue: true,required: true
	input name: "debugLogging", type: "bool", title: "Enable debug logging", description: "Programming Debug logs" ,defaultValue: false,required: true
	input name: "traceLogging", type: "bool", title: "Enable trace logging", description: "Insane HIGH level", defaultValue: false,required: true
if (state.parameter[8]){
    input(  "heatDiff", "number", title: "2 Stage Heat differential", description: "Only for 2 stage ElectHeat or HeatPumps. 2nd stage engages when x above setpoint.(2 to 6)", defaultValue: 2,required: true)
    input(  "coolDiff", "number", title: "2 Stage Cool differential", description: "Only for 2 stage HeatPumps. (ignore if not using)(2 to 6)", defaultValue: 2,required: true)
}
if (state.parameter[7]){input(  "swing", "enum", title: "Temperature Swing", description: "Number of degrees above (for cooling) and below (for heating) the temp will fluctuate before cycling back on.", options: ["0.5","1.0","1.5","2.0","2.5","3.0","3.5","4.0"], defaultValue: "1.0", multiple: false, required: true)}
if (state.parameter[9]){input name: "recovery", type: "enum", title: "Recovery mode", description: "Fast or economy. ",  options: ["fast", "economy"], defaultValue: "economy",required: true }
    input name: "onlyMode", type: "enum", title: "Mode Bypass", description: "Heat or Cool only mode",  options: ["off", "heatonly","coolonly"], defaultValue: "off",required: true 
    input(  "polling", "enum", title: "Polling minutes", description: "Polling Chron. Press Config after changing ", options: ["5","10","15","20","25","30","35","40","45","50","55",],defaultValue: 15,required: true)


    input name: "autocorrect", type: "bool", title: "Auto Correct setpoints", description: "Keep thermostat settings matching hub (this will overide local changes)", defaultValue: false,required: true
    input(  "autocorrectNum", "number", title: "Auto Correct errors", description: "send auto corect after number of errors detected. ", defaultValue: 5,required: true)

input name: "ignorebat", type: "bool", title: "Ignore Bat reports", description: "If no batteries inserted. Batteries are not needed if main powered.", defaultValue: false,required: true
    

}

def installed(){
logging("Radio Thermostat Paired!", "info")
cleanState()    
configure()
}

// Runs on reboot
def initialize(){
    logging("Radio Thermostat Initialize ", "debug")
  	randomSixty = Math.abs(new Random().nextInt() % 180)
	runIn(randomSixty,refresh)
}


void cleanState(){
    state.remove("paypal") 
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
removeDataValue("supportedFanModes")    
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
    getIcons()
    setClock()
    updateParameters()
    logging("Configure Driver v${state.version}", "info")
    fanAuto() // set default
	updated()
    state.cwire =0 
    state.remove("lastBatteryGet")
    logging("Sending >> configurationSet (parameterNumber: 4, size: 1, configurationValue: [2])", "trace")  
    logging("Sending >> configurationGet (4,7,8,9 = cwire,swing,diff,recovery", "trace")
    logging("Sending >> get request (man,mode,FanMode) ", "trace")
    delayBetween([
        zwave.associationV1.associationSet(groupingIdentifier:1, nodeId:1).format(), // get reports without polling
        zwave.manufacturerSpecificV2.manufacturerSpecificGet().format(),// fingerprint
		zwave.thermostatModeV2.thermostatModeSupportedGet().format(),
        zwave.thermostatFanModeV3.thermostatFanModeSupportedGet().format(),
        zwave.configurationV2.configurationSet(parameterNumber: 4, size: 1, configurationValue: [2]).format(), // cwire enabled
        zwave.configurationV2.configurationGet(parameterNumber: 4).format(), // is cwire 1=true 2=false
        zwave.configurationV2.configurationGet(parameterNumber: 7).format(), // swing        
        zwave.configurationV2.configurationGet(parameterNumber: 8).format(), // is diff
        zwave.configurationV2.configurationGet(parameterNumber: 9).format(), // is fast recovery on ? 1on 0 off
        zwave.versionV1.versionGet().format(),
	], 3500)
}


def updated() {
    clientVersion()
    updateParameters()
    if (!polling){polling=15}    // Poll the device every x min
    // options: ["10","15","20","30","40","50"]
    int checkEveryMinutes = Integer.parseInt(polling)
	randomSixty = Math.abs(new Random().nextInt() % 60)
	schedule("${randomSixty} 0/${checkEveryMinutes} * * * ? *", poll)
    schedule("${randomSixty} 0 12 * * ? *", setTheClock)
    logging("Setting Chron Poll: every ${checkEveryMinutes}mins  Clock: 12:${randomSixty}", "info")

    loggingUpdate()
    logging("Sending >> get request (mode,temp,state) ", "trace")
    saveSettings()
    delayBetween([
    zwave.thermostatModeV2.thermostatModeGet().format(),// get mode
    zwave.thermostatOperatingStateV1.thermostatOperatingStateGet().format(),// get state
    zwave.sensorMultilevelV3.sensorMultilevelGet().format(), // current temperature
    zwave.batteryV1.batteryGet().format()    
    ], 3500)    
}

def ping() {
    logging("Ping", "info")
    logging("Sending >> get request (mode,temp,state) ", "trace")
    updateParameters()
    delayBetween([
    zwave.thermostatModeV2.thermostatModeGet().format(),// get mode
    zwave.thermostatOperatingStateV1.thermostatOperatingStateGet().format(),// get state
    zwave.sensorMultilevelV3.sensorMultilevelGet().format(), // current temperature
    zwave.batteryV1.batteryGet().format()    
    ], 3500)      
}    

def pollDevice() {
    logging("pollDevice", "info")
    poll()
}

def refresh() {
    logging("refresh", "info")
    poll()
}
def poll() {
    clientVersion()

    updateParameters()
    def nowCal = Calendar.getInstance(location.timeZone)
    Timecheck = "${nowCal.getTime().format("EEE MMM dd", location.timeZone)}"
    logging("Poll E=Event# ${Timecheck} v${state.version}", "info")
//zwave.sensorMultilevelV5.sensorMultilevelGet().format(), // testing
//zwave.batteryV1.batteryGet().format(),
//zwave.commands.versionv2.VersionReport
//zwave.thermostatModeV2.thermostatModeSupportedGet().format(),
//zwave.manufacturerSpecificV2.manufacturerSpecificGet().format(),

logging("Sending >> get request (temp,Hum,Setpoint 1&2,mode,FanMode,state,battery) ", "trace")
logging("Sending >> configurationGet (4,7,8,9 = cwire,swing,diff,recovery", "trace")
   
	delayBetween([
		zwave.sensorMultilevelV3.sensorMultilevelGet().format(),// current temperature
        zwave.multiInstanceV1.multiInstanceCmdEncap(instance: 2).encapsulate(zwave.sensorMultilevelV2.sensorMultilevelGet()).format(), // CT-100/101 Humidity
		zwave.thermostatSetpointV1.thermostatSetpointGet(setpointType: 1).format(),
		zwave.thermostatSetpointV1.thermostatSetpointGet(setpointType: 2).format(),
		zwave.thermostatModeV2.thermostatModeGet().format(),
		zwave.thermostatOperatingStateV1.thermostatOperatingStateGet().format(),
        zwave.thermostatFanModeV3.thermostatFanModeGet().format(),
        zwave.configurationV2.configurationGet(parameterNumber: 1).format(),// get Temperature Reporting Threshold
        zwave.configurationV2.configurationGet(parameterNumber: 2).format(), // HVAC Settings
        zwave.configurationV2.configurationGet(parameterNumber: 4).format(),// get cwire
        zwave.configurationV2.configurationGet(parameterNumber: 5).format(), // Humidity Reporting Threshold
        zwave.configurationV2.configurationGet(parameterNumber: 6).format(),// get emergency aux
        zwave.configurationV2.configurationGet(parameterNumber: 7).format(),// temp swing
        zwave.configurationV2.configurationGet(parameterNumber: 8).format(),// is temp diff
        zwave.configurationV2.configurationGet(parameterNumber: 9).format(),// is fast recovery
        zwave.batteryV1.batteryGet().format()
//        zwave.configurationV2.configurationGet(parameterNumber: 11).format(),// Get UI mode
	], 3500)// delay increased to allow therm to respond.
// zwave.configurationV2.configurationGet(parameterNumber: 10).format(),// reports data what is it
// These are documented 
//   zwave.configurationV2.configurationGet(parameterNumber: 3).format(), // Utility Lock
//   zwave.configurationV2.configurationGet(parameterNumber: 11).format(),// get UI mode 
//    zwave.configurationV2.configurationSet(parameterNumber: 11, size: 1, configurationValue: 1) // simple UI enabled 1 on 2 off
    
}


def updateParameters(){// set the info line
    text =""
    if(!state.parameter) {state.parameter=[]}
    state.parameter[0] =0
    state.parameter[3] =0
    state.parameter[10] =0
    if(!state.parameter[7]) {state.parameter[7]=0}
    if(!state.parameter[8]) {state.parameter[8]=0}
    if(!state.parameter[8]) {state.parameter[9]=0}
    if(!state.parameter[14]) {state.parameter[14]=0}
    if(!state.parameter[11]) {state.parameter[11]=0}
    if(!state.parameter[17]) {state.parameter[17]=0}
    if(!state.parameter[18]) {state.parameter[18]=0}
    
    if (state.parameter[1]){text +="TempReportThreshold,"}
    if (state.parameter[2]){text +="HVAC,"}
    if (state.parameter[3]){text +="Lock,"}
    if (state.parameter[4]){text +="CWIRE,"}
    if (state.parameter[5]){text +="HumReportThreshold,"}
    if (state.parameter[6]){text +="AuxHeat,"}
    if (state.parameter[7]){text +="Swing,"}
    if (state.parameter[8]){text +="Diff,"}
    if (state.parameter[9]){text +="Recovery,"}
    if (state.parameter[10]){text +="unknown,"}
    if (state.parameter[11]){text +="UI,"}
    if (state.parameter[12]){text +="Temp,"}
    if (state.parameter[13]){text +="Hum,"}
    if (state.parameter[14]){text +="Battery,"}
    if (state.parameter[15]){text +="Clock,"}
    if (state.parameter[16]){text +="FanState,"}
    if (state.parameter[17]){text +="energySaveHeat,"}
    if (state.parameter[18]){text +="energySaveCool"}
    
    state.info ="Supported Parameters: ${text}"
}

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
   CommandClassCapabilities = [0x31:3,0x40:2,0x42:1,0x43:2,0x44:3,0x45:1,0x60:3,0x70:2,0x72:2,0x80:1,0x81:1,0x85:1,0x86:1,0x98:1]   
   hubitat.zwave.Command map = zwave.parse(description, CommandClassCapabilities)
   if (!map) {
   logging("Unable to Parse ${description}", "error")   
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
    
    if (!cmd){
        logging("E1 Received NULL", "warn")
        return
    }
    logging("E1 Received ${cmd}", "debug")
    logging("E1 Received setpointType:${cmd.setpointType} scaledValue:${cmd.scaledValue} precision:${cmd.precision} scale:${cmd.scale} ","trace")
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
      //state.error = 0 
      }
     if(tempCheck != tempCheck2 & autocorrect == true){
         state.error = state.error + 1
         logging("E1 ${map.name} Last Point does not match Last${tempCheck}<>Current${tempCheck2} error:${state.error} fixOn:${autocorrectNum}", "warn")
         if (state.error >= autocorrectNum ){
          runIn(1,FixHeat)
          runIn(5,FixCool)
          logging("Resetting cool:${state.SetCool}/Heat:${state.SetHeat}", "info")
          state.error = 0    
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
	logging("E2 Received sensorMultilevelv2", "debug")
//    logging("E2 Received sensorType:${cmd.sensorType} scaledSensorValue:${cmd.scaledSensorValue} scale:${cmd.scale}","trace")
    def map = [:]
    map.displayed = true
    map.isStateChange = true
    if (cmd.sensorType == 0) {return }  // ct30 (fixed in rev1)
	if (cmd.sensorType == 1) {
		map.value = convertTemperatureIfNeeded(cmd.scaledSensorValue, cmd.scale == 1 ? "F" : "C", cmd.precision)
		map.unit = getTemperatureScale()
		map.name = "temperature"
        state.parameter[12]=1
	} else if (cmd.sensorType == 5) {
		map.value = cmd.scaledSensorValue
		map.unit = "%"
		map.name = "humidity"
        state.parameter[13]=1
	}
    if (map.value != null){
    logging("E2 ${map.name} ${map.value} ${map.unit}", "info")
    sendEvent(name:map.name ,value:map.value ,unit: map.unit, descriptionText:"${map.name} ${map.value} ${state.version}", isStateChange: true)
    }
    else {logging("E2 Unknown data ${cmd}", "warn")}
	
}
// E3
def zwaveEvent(hubitat.zwave.commands.thermostatoperatingstatev1.ThermostatOperatingStateReport cmd)
{
	def map = [:]
    logging("E3 Received State ${cmd.operatingState}", "debug")
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
    
     if(state.model =="CT30e bad"){
        logging("E3 Received State. Ignoring defective firmware ${state.model}", "debug")
        map.value = "idle"
       } 	
    logging("E3 ${map.name} - ${map.value} ", "info2")
    sendEvent(name: map.name, value: map.value,descriptionText: "${map.name} ${map.value} ${state.version}", isStateChange:true)

// --------------simulate fan state for ct sending bad data--------------------
    fanTest  = device.currentValue("thermostatFanMode")
    if(fanTest == "fanAuto"){ // fan should follow operating state if in auto
     if (map.value == "idle"){ value = "idle"}
        else {value = "running"}  
    }   
    else if (fanTest == "fanOn"){value = "running"}     
    logging("E3 Fan State Simulated: ${value} ", "info2")
    sendEvent(name: "thermostatFanState", value: value,descriptionText: "Simulated FanState ${value} ${state.version}", isStateChange:true)
}
// Fan State Report 
def zwaveEvent(hubitat.zwave.commands.thermostatfanstatev1.ThermostatFanStateReport cmd) {
    logging("E4 Received Fan State ${cmd.fanOperatingState}", "debug")
    state.parameter[16]=1
    if(cmd.fanOperatingState ==0){value = "idle"}
    else if(cmd.fanOperatingState ==1){value = "running"}
    else if(cmd.fanOperatingState ==2){value = "running high"}
    else {
        logging("E4 Received Unknown Fan State :${cmd.fanOperatingState} ", "warn")
        return
    }
    logging("E4 Received Fan State :${value} ", "info2")
    sendEvent(name: "thermostatFanState", value: value,descriptionText: "Fan State ${value} ${state.version}", isStateChange:true)
	
}

//off, heat, cool, auto, auxiliaryemergencyHeat, resume, fanOnly, furnace, energySaveHeat, energySaveCool, away
def zwaveEvent(hubitat.zwave.commands.thermostatmodev2.ThermostatModeReport cmd) {
	def map = [:]
    logging("E5 Received Mode:${cmd.mode}", "debug")
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
}
// auto, low, autoHigh, high, autoMedium, medium, circulation, humidityCirculation
def zwaveEvent(hubitat.zwave.commands.thermostatfanmodev3.ThermostatFanModeReport cmd) {
	def map = [:]
    logging("E6 Received FanMode:${cmd.fanMode}", "debug")
    map.name = "thermostatFanMode"
    map.value = "unknown"
	switch (cmd.fanMode) {
		case hubitat.zwave.commands.thermostatfanmodev3.ThermostatFanModeReport.FAN_MODE_AUTO_LOW:// =0
			map.value = "fanAuto"
			break
        case hubitat.zwave.commands.thermostatfanmodev3.ThermostatFanModeReport.FAN_MODE_AUTO_HIGH:
			map.value = "fanAuto"
			break
		case hubitat.zwave.commands.thermostatfanmodev3.ThermostatFanModeReport.FAN_MODE_LOW: // =1
			map.value = "fanOn"
			break
        case hubitat.zwave.commands.thermostatfanmodev3.ThermostatFanModeReport.FAN_MODE_HIGH: 
			map.value = "fanOn"
			break
		case hubitat.zwave.commands.thermostatfanmodev3.ThermostatFanModeReport.FAN_MODE_CIRCULATION:// Dont thinmk rT supports this
			map.value = "fanCirculate"
			break
	}
	
    logging("E6 ${map.name} - ${map.value} ", "info2")
    sendEvent(name: map.name, value: map.value,descriptionText: "${map.name} ${map.value} ${state.version}", isStateChange:true)
 
// This is simulated due to ct not sending fan state
    if (map.value == "fanOn" || map.value == "fanCirculate" ){ 
        value = "running"
        logging("E6 Simulated Fan State: ${value} ", "info2")
        sendEvent(name: "thermostatFanState", value: value,descriptionText: "Simulated FanState ${value} ${state.version}", isStateChange:true)
    }

}
//off:true, heat:true, cool:true, auto:true, auxiliaryemergencyHeat:false, resume:false, fanOnly:false, furnace:false, energySaveHeat:false, energySaveCool: false, away:false
//off:false, heat:false, cool:false, auto:false, auxiliaryemergencyHeat:false, resume:false, fanOnly:false, furnace:false, energySaveHeat:true, energySaveCool: true, away:false)
def zwaveEvent(hubitat.zwave.commands.thermostatmodev2.ThermostatModeSupportedReport cmd) {
	def supportedModes = ""
    logging("E7 Received ${cmd}", "debug")
    if(cmd.energySaveHeat) { state.parameter[17]=1 }
    if(cmd.energySaveCool) { state.parameter[18]=1 }
    
    
	if(cmd.off) { supportedModes += '"off"' }
	if(cmd.heat) { supportedModes += ',"heat"' }
	if(cmd.auxiliaryemergencyHeat) { supportedModes += ',"emergency heat"' }
    if(cmd.cool) { supportedModes += ',"cool"' }
    if(cmd.auto) { supportedModes += ',"auto"' }
//off:false, heat:false, cool:false, auto:false, auxiliaryemergencyHeat:false, resume:false, fanOnly:false, furnace:false, energySaveHeat:true, energySaveCool: true, away:false)
    if(cmd.energySaveHeat) { supportedModes += ',"energySaveHeat"' } // CT30 bad reports these then reports it doesnt have them?
    if(cmd.energySaveCool) { supportedModes += ',"energySaveCool"' }
     
    
    
    

// custom setup for heat or cool only    
 	if(onlyMode == "coolonly"){supportedModes = '"off","cool"'}
 	if(onlyMode == "heatonly"){supportedModes = '"off","heat"'
     if(cmd.auxiliaryemergencyHeat) { supportedModes += ',"emergency heat"' }
    }        
    logging("E7 supportedModes [${supportedModes}]", "info2")
    sendEvent(name: "supportedThermostatModes", value: "[${supportedModes}]",descriptionText: "${supportedModes} ${state.version}", isStateChange:true)
//  new firmware update requires quotes ["off", "heat", "cool", "auto", "emergency heat"] 
    
}
// auto:true, low: true, autoHigh: false, high: false autoMedium:false, medium:false circulation:false, humidityCirculation:false
def zwaveEvent(hubitat.zwave.commands.thermostatfanmodev3.ThermostatFanModeSupportedReport cmd) {
	def supportedFanModes = '' // ["Auto","On"]
    logging("E8 Received ${cmd}", "debug")
	if(cmd.auto) { supportedFanModes += '"Auto",' }
	if(cmd.low) { supportedFanModes += '"On"' }
	if(cmd.circulation) { supportedFanModes += ',"Circulate",' } // not used
    if(cmd.humidityCirculation){supportedFanModes += "HumCirculate, " } // not used
 	if(cmd.high) { supportedFanModes += "High," } // not used
    logging("E8 supportedFanModes[${supportedFanModes}]", "info2")

    sendEvent(name: "supportedThermostatFanModes", value: "[${supportedFanModes}]",descriptionText: "${supportedFanModes} ${state.version}", isStateChange:true)
//supportedThermostatFanModes/
    
}
// these are untrapped log them...
def zwaveEvent(hubitat.zwave.commands.basicv1.BasicReport cmd) {
    logging("E9 Received ${cmd}", "debug")
}

def zwaveEvent(hubitat.zwave.commands.configurationv2.ConfigurationReport cmd) {
    logging("E10 Received ${cmd}", "debug")
    if(!state.parameter){state.parameter=[]}
    if (cmd.parameterNumber== 1){//0 to 4
        state.parameter[1]=1 
        def value = cmd.scaledConfigurationValue
//        def test = cmd2Integer(cmd.configurationValue)
        if (value <=5 && value >=0){logging("E10-1 Temp Report Threshold ${value}", "info2")}
        else {
            if (debugLogging){logging("E10-1 ERROR Temp Report Threshold value:${cmd.configurationValue} Scaled:${cmd.scaledConfigurationValue} Not a valid value", "warn")} 
        }
    }
//2	4	HVAC Settings	1 to 2147483647	Byte 1: normal (1) or heat pump (2). Byte 2, Bits 7-4: Gas (1) or Electric (2). Byte 2, Bits 3-0: # of Auxiliary Stages. 
//     Byte 3: # of Heat Pump Stages. Byte 4: # of Cool Stages. value:[1, 34, 0, 1] Scaled:19005441
    else if (cmd.parameterNumber== 2){
    state.parameter[2]=1    
    if(cmd.size == 4){
    mode = cmd.configurationValue[0]
    gas  = cmd.configurationValue[1] // dont yet know how to seperate bits 7-4 and 3-0  need help...(34= electric and 2 stages.)
    pump  = cmd.configurationValue[2]
    cool  = cmd.configurationValue[3]
        
    modet="Normal"
    gasT=""
    if (mode == 2){modet="Heat Pump"}
    if (gas == 34){gasT= "2 Stage Electric"}
    if (gas == 33){gasT= "Backup Electric"}    
    logging("E10-2 HVAC Settings mode:${modet} ${gasT} HeatPumpStages:${pump} CoolStages:${cool}", "info2")
    }    
    else {// size 3 is not in the spec but I get 1,1,1 from a ct30
    mode = cmd.configurationValue[0]
    modet="Normal"
    if (mode == 2){modet="Heat Pump"}
    logging("E10-2 HVAC Settings mode:${modet}", "info2")    
        }
    logging("E10-2 HVAC Settings value:${cmd.configurationValue} Scaled:${cmd.scaledConfigurationValue}", "debug")
    }
    
//  ConfigurationReport(parameterNumber: 4, size: 1, configurationValue: [1], scaledConfigurationValue: 1)
    else if (cmd.parameterNumber== 4){
        state.parameter[4]=1
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
// Humidity Reporting Threshold	0 to 3	0 = Disabled, 1 = 3% RH, 2 = 5% RH, 3 = 10% RH 
// E10 Received ConfigurationReport(parameterNumber: 5, size: 1, configurationValue: [2], scaledConfigurationValue: 2)   
    else if (cmd.parameterNumber== 5){
        state.parameter[5]=1
        if (cmd.scaledConfigurationValue == 0){report="Disabled"}
        if (cmd.scaledConfigurationValue == 1){report="3% RH"}
        if (cmd.scaledConfigurationValue == 2){report="5% RH"}
        if (cmd.scaledConfigurationValue == 3){report="10% RH"}
       logging(" E10-5 Humidity Reporting Threshold :${report} ", "info2")
  
   }        
    
    
//  Auxiliary/Emergency Mode 0 = Disabled, 1 = Enabled  
       else if (cmd.parameterNumber== 6){ 
           state.parameter[6]=1
           if (cmd.configurationValue == 1){logging("E10-6 Auxiliary/Emergency TRUE", "info2")}    
           else { logging("E10-6 Auxiliary/Emergency FALSE", "info2")}  
       }
    
//  ConfigurationReport(parameterNumber: 7, size: 1, configurationValue:

    else if (cmd.parameterNumber== 7){
    state.parameter[7]=1   
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
    
//  2 to 6(Default)	The thermostat differential temperature is in units of 0.5 degrees Fahrenheit.   
   else if (cmd.parameterNumber== 8){
     state.parameter[8]=1   
     def testHeat = cmd.configurationValue[0] * 0.5
     def testCool = cmd.configurationValue[1] * 0.5  
       logging("E10-8 DiffReport Heat:${testCool}° Cool:${testHeat}°  [RawHeat:${cmd.configurationValue[1]} RawCool:${cmd.configurationValue[1]}] ", "debug")  
// -------------heat       
     if (state.heatDiff == testHeat) {
         logging("E10-8-0 (2 stage Heat Differential) :${testHeat} °", "info2")
         sendEvent(name: "recoveryTempDiffHeat", value: "${testHeat}",descriptionText: "heat ${testHeat}°  ${state.version}",displayed: true, isStateChange:true)
     }
     else {
           if (debugLogging){
           logging("E10-8-0 Report ERROR (2 stage Differential) Heat:${testHeat} [should be ${state.heatDiff}]", "warn")
           }
     }   
// ------------cool       
     if (state.coolDiff == testCool) {
         logging("E10-8-1 (2 stage Cool Differential) :${testCool} °", "info2")
         sendEvent(name: "recoveryTempDiffCool", value: "${testCool}", descriptionText: "cool ${testCool}  ${state.version}",displayed: true, isStateChange:true)
     }
     else {
           if (debugLogging){
           logging("E10-8-1 Report ERROR (2 stage Differential) Cool:${testCool} [should be ${state.coolDiff}]", "warn")
           }
       }
    
   }
    
    
// ConfigurationReport(parameterNumber: 9, size: 1, configurationValue: [2], scaledConfigurationValue: 2)  Fast Recovery
   else if (cmd.parameterNumber== 9){
       state.parameter[9]=1
       if (cmd.configurationValue[0] == 1){state.fastrecovery = "fast"}
       if (cmd.configurationValue[0] == 2){state.fastrecovery = "economy"} 
       logging(" E10-9 Recovery :${state.fastrecovery} #${cmd.configurationValue[0]}", "info2")
       sendEvent(name: "recovery", value: "${state.fastrecovery}", descriptionText: "${state.fastrecovery}  ${state.version}",displayed: true, isStateChange:true)
 
   }  
    

    
    
    
    else {logging("E10 Untraped parameterNumber:${cmd.parameterNumber}", "warn")}  
    updateParameters()
}

/**
 *  zwaveEvent( COMMAND_CLASS_MANUFACTURER_SPECIFIC V2 (0x72) : MANUFACTURER_SPECIFIC_REPORT (0x05) )
 *
 *  Manufacturer-Specific Reports are used to advertise manufacturer-specific information, such as product number
 *  and serial number.
 **/
def zwaveEvent(hubitat.zwave.commands.manufacturerspecificv2.ManufacturerSpecificReport cmd) {
  logging("${cmd} ", "debug")
    
	def map = [:]
    map.mfr   = hubitat.helper.HexUtils.integerToHexString(cmd.manufacturerId, 2)
    map.model = hubitat.helper.HexUtils.integerToHexString(cmd.productId, 2)
    map.type  = hubitat.helper.HexUtils.integerToHexString(cmd.productTypeId, 2)
    logging("fingerprint mfr:${map.mfr} prod:${map.type} model:${map.model}", "debug")
   
//   state.remove("fingerprint")
    
   state.model ="unknown"
    
    if (map.type=="1E12" | map.type=="1E10" | map.type=="0000" | map.type=="0001"){
      state.model ="CT30"  
      if (map.model=="015E") {state.model ="CT30e rev.01"}
      if (map.model=="015C") {state.model ="CT30e"}
      if (map.model=="001E") {state.model ="CT30e bad"}  
    } 
    
    if (map.type=="2002" ){ state.model ="CT32"}// 0002:0100,2002:0100,2002:0102 
    if (map.type=="0002" ){ state.model ="CT32"}
    if (map.type=="3200" ){ state.model ="CT50"}
    if (map.type=="5003" ){ state.model ="CT80"}
    if (map.type=="6401" || map.type=="6402" || map.type=="0015"){ state.model ="CT100"} 
    if (map.type=="6402" || map.type=="0100"){ state.model ="CT100 Plus"}
    if (map.type=="6501"){   
      state.model ="CT101"
      if (map.model=="000B") {state.model ="CT101 iris"}
   }
    if (map.type=="C801"){
        state.model = "CT200"
        if(map.model =="001D"){state.model ="CT200 Vivant"}
        if(map.model =="0022"){state.model = "CT200x Vivant"}
    }
    if (map.type=="6E01"){state.model = "CT110"} // model=0000

    
    logging("fingerprint ${state.model} [${map.mfr}-${map.type}-${map.model}] ", "info")
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
// VersionReport(zWaveLibraryType:3, zWaveProtocolVersion:2, zWaveProtocolSubVersion:78, applicationVersion:5, applicationSubVersion:0)
// VersionReport(zWaveLibraryType:3, zWaveProtocolVersion:3, zWaveProtocolSubVersion:28, applicationVersion:9, applicationSubVersion:0)   
def zwaveEvent(hubitat.zwave.commands.versionv1.VersionReport cmd) {
  
//    device.updateDataValue("firmwareVersion",   "${cmd.firmware0Version}.${cmd.firmware0SubVersion}")
    device.updateDataValue("protocolVersion",   "${cmd.zWaveProtocolVersion}.${cmd.zWaveProtocolSubVersion}")
    device.updateDataValue("applicationVersion","${cmd.applicationVersion}.${cmd.applicationSubVersion}")
//    device.updateDataValue("hardwareVersion",   "${cmd.hardwareVersion}")
    logging("E12 VersionReport v1 protocolVersion: ${cmd.zWaveProtocolVersion}.${cmd.zWaveProtocolSubVersion} applicationVersion: ${cmd.applicationVersion}.${cmd.applicationSubVersion}", "info2")
    
}



def zwaveEvent(hubitat.zwave.Command cmd ){
    
  if (debugLogging){logging("E11 Received Untrapped (${cmd})", "warn")}    
    

}


    
// have yet to see data here
def zwaveEvent(hubitat.zwave.commands.versionv2.VersionReport cmd) {
    logging("Received E12 v2${cmd}", "info")
    device.updateDataValue("firmwareVersion", "${cmd.firmware0Version}.${cmd.firmware0SubVersion}")
    device.updateDataValue("protocolVersion", "${cmd.zWaveProtocolVersion}.${cmd.zWaveProtocolSubVersion}")
    device.updateDataValue("hardwareVersion", "${cmd.hardwareVersion}")
}

//==================heating
def FixHeat(){
//state.SetCool
 logging("Recovery HEATing Setpoint", "warn")   
 setHeatingSetpoint(state.SetHeat)
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
//  we cant create event here must wait for the therm to report back.  
//  sendEvent(name: "thermostatSetpoint", value: convertedDegrees, unit:locationScale ,descriptionText: "Reset Last to ${state.SetHeat} ${state.version}", isStateChange:true)
    
     
    
    logging("Sending >> Set (heat type=1) scale:${deviceScale}, precision:${p},  scaledValue:${convertedDegrees}  get(temp)", "trace")
	delayBetween([
		zwave.thermostatSetpointV1.thermostatSetpointSet(setpointType: 1, scale: deviceScale, precision: p, scaledValue: convertedDegrees).format(),
        zwave.thermostatSetpointV1.thermostatSetpointSet(setpointType: 1, scale: deviceScale, precision: p, scaledValue: convertedDegrees).format(),
		zwave.thermostatSetpointV1.thermostatSetpointGet(setpointType: 1).format(),
        zwave.sensorMultilevelV3.sensorMultilevelGet().format(),// current temperature
        zwave.thermostatModeV2.thermostatModeGet().format(),// mode update dashboard
        zwave.thermostatOperatingStateV1.thermostatOperatingStateGet().format(),
	], 2500)
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
    logging("Set Cool Setpoint ${convertedDegrees} ${locationScale} ---Reset Last to ${state.SetCool}  Get(temp)", "info")
    sendEvent(name: "SetCool", value: convertedDegrees, unit:locationScale ,descriptionText: "Reset Last to ${state.SetCool} ${state.version}", isStateChange:true)

//  we cant create event here must wait for the therm to report back.    
//  sendEvent(name: "thermostatSetpoint", value: convertedDegrees, unit:locationScale ,descriptionText: "Reset Last to ${state.SetCool} ${state.version}", isStateChange:true)

    
    
    logging("Sending >> Set (cool type=2) scale:${deviceScale}, precision:${p},  scaledValue:${convertedDegrees} Get(temp)", "trace")

    
	delayBetween([
		zwave.thermostatSetpointV1.thermostatSetpointSet(setpointType: 2, scale: deviceScale, precision: p, scaledValue: convertedDegrees).format(),
		zwave.thermostatSetpointV1.thermostatSetpointSet(setpointType: 2, scale: deviceScale, precision: p, scaledValue: convertedDegrees).format(),
        zwave.thermostatSetpointV1.thermostatSetpointGet(setpointType: 2).format(),
        zwave.sensorMultilevelV3.sensorMultilevelGet().format(),// current temperature
        zwave.thermostatModeV2.thermostatModeGet().format(),// mode update dashboard
        zwave.thermostatOperatingStateV1.thermostatOperatingStateGet().format(),
	], 2500)
}



def getDataByName(String name) {
	state[name] ?: device.getDataValue(name)
}

// E20 receives Hub mode command
def setThermostatMode(String value) {
    logging("E20 setThermostatMode  ${value}", "debug")
    if(value == "off"){ set= 0}
    else if(value == "heat"){ set = 1}
    else if(value == "cool"){ set = 2}
    else if(value == "auto"){ set = 3}
    else if(value == "energySaveHeat"){set = 1 }
    else if(value == "energySaveCool"){set = 2 }
    
    
    
    else if(value == "emergency heat"){set = 4}
    // process heat cool or only
    else if(value == "heat" && onlyMode == "coolonly"){ 
       coolOnly()
       return
    }    
    else if(value == "emergency heat" && onlyMode == "coolonly"){
       coolOnly()
       return
     } 
    else if(value == "cool" && onlyMode == "heatonly"){
       heatingOnly()
       return
    }
    
    else if(value == "auto"){
       if(onlyMode == "heatonly" || onlyMode =="coolonly"){
       noAuto()    
       return
       }
    }    
    else {return}
 

    logging("Sending >>  thermostatModeSet ${set} Mode:${value} Get(mode,temp)", "trace")    
    logging("E20 SetMode:${value}", "info")
	delayBetween([
		zwave.thermostatModeV2.thermostatModeSet(mode: set).format(),
		zwave.thermostatModeV2.thermostatModeGet().format(),
        zwave.sensorMultilevelV3.sensorMultilevelGet().format(),// current temp
        zwave.thermostatOperatingStateV1.thermostatOperatingStateGet().format(),
	], 2500)
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

void noAuto(){logging("When in ${onlyMode} auto disabled", "info") }
def off()  {setThermostatMode("off")}
def heat() {setThermostatMode("heat")}
def cool() {setThermostatMode("cool")}
def auto() {setThermostatMode("auto")}
def emergencyHeat() {setThermostatMode("emergency heat")}


// fan settings------------------------------
// E21
def setThermostatFanMode(String value) {
    logging("E21 setThermostatFanMode ${value}", "debug")
    if(value == "Auto"){   set=0}
    else if(value == "On"){set=1}
    else {
    runIn(2,configure) // We need to reconfig the values are not set correct  
    return
    }
    logging("E21 SetFanMode:${value}", "info")
    logging("Sending >>  thermostatFanModeSet ${set} Mode:${value} Get(FanMode,temp)", "trace")  
	delayBetween([
		zwave.thermostatFanModeV3.thermostatFanModeSet(fanMode: set).format(),
        zwave.thermostatModeV2.thermostatModeGet().format(),// mode Do first to set simulated fan state
		zwave.thermostatFanModeV3.thermostatFanModeGet().format(),// fan mode
        zwave.sensorMultilevelV3.sensorMultilevelGet().format(),// current temp
        zwave.thermostatOperatingStateV1.thermostatOperatingStateGet().format(),
	], 2500)   
}
def fanOn()        {setThermostatFanMode("On")}
def fanAuto()      {setThermostatFanMode("Auto")}
def fanCirculate() {setThermostatFanMode("Circulate")}



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
    state.parameter[14]=1
    if (cmd.batteryLevel == 0xFF){ // I have never seen this but its in the spec.
        logging("---- Power Restored ----", "info")
        sendEvent(name: "powerSource", value: "mains",descriptionText: "Power Mains ${state.version}", isStateChange: true)
        return
    }

    if(ignorebat==true){
        logging("E15 Ignoring battery ${cmd.batteryLevel}% Set to 100%", "debug")
        sendEvent(name: "battery", value: 100 ,unit: "%", descriptionText: "${cmd.batteryLevel}% ${state.version}", isStateChange:true)
        return
     }
      
                
    logging("E15 battery ${cmd.batteryLevel}% ", "info")
    sendEvent(name: "battery", value: cmd.batteryLevel ,unit: "%", descriptionText: "${cmd.batteryLevel}% ${state.version}", isStateChange:true)
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
    logging("Adjusting clock (${theTime}) ${state.LastTimeSet}", "info")
    logging("Sending >>  clockSet (hour: ${nowCal.get(Calendar.HOUR_OF_DAY)}, minute: ${nowCal.get(Calendar.MINUTE)}, weekday: ${weekdayZ}) Get(clock,bat)", "trace")    
    sendEvent(name: "SetClock", value: theTime, descriptionText: "${theTime} ${state.version}",displayed: true, isStateChange:true)

        delayBetween([
		zwave.clockV1.clockSet(hour: nowCal.get(Calendar.HOUR_OF_DAY), minute: nowCal.get(Calendar.MINUTE), weekday: weekdayZ).format(),
        zwave.clockV1.clockGet().format(),
        zwave.batteryV1.batteryGet().format()    
	], 2500)

    
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
    state.parameter[15]=1
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

def saveSettings(){
 logging("Updating Settings", "debug") 

 runIn(2,setDiff)  
 runIn(4,setSwing)
 runIn(6,setRecovery)
 runIn(10,setTheClock)

}


def setDiff(cmd){
   if (!state.parameter[8]){ return}
// 2 stage differential (2 to 6) default
   coolDiff = (coolDiff as Integer) 
   heatDiff = (heatDiff as Integer) 
   if (!coolDiff){coolDiff = 2}
   if (!heatDiff){heatDiff = 2} 
   if (coolDiff >6){coolDiff =2}
   if (heatDiff >6){coolDiff =2} 
   if (coolDiff >2){coolDiff =2}
   if (heatDiff >2){coolDiff =2}  
   state.heatDiff = heatDiff
   state.coolDiff = coolDiff 
   logging("Set (2 stage Differential) Heat:${heatDiff} Cool:${coolDiff}", "info")
   logging("Sending >>  configurationSet (parameterNumber: 8, size: 2, configurationValue: [0x00, ${heatDiff}]) ", "trace")    
   logging("Sending >>  configurationSet (parameterNumber: 8, size: 2, configurationValue: [0x01, ${coolDiff}])  Get(config 8)", "trace")  
    delayBetween([    
   zwave.configurationV2.configurationSet(parameterNumber: 8, size: 2, configurationValue: [0x00, heatDiff]).format(),
   zwave.configurationV2.configurationSet(parameterNumber: 8, size: 2, configurationValue: [0x01, coolDiff]).format(), 
   zwave.configurationV2.configurationGet(parameterNumber: 8).format(),    
	], 2500)    

}

def setSwing(cmd){
    if (!state.parameter[7]){ return}
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

  logging("Sending >>  configurationSet (parameterNumber: 7, size: 1, configurationValue: [${value}])  Get(config 7)", "trace")      
   delayBetween([    
   zwave.configurationV2.configurationSet(parameterNumber: 7, size: 1, configurationValue: [value]).format(),
   zwave.configurationV2.configurationGet(parameterNumber: 7).format(),    
	], 2500)  


}

def setRecovery(cmd){
    if (!state.parameter[9]){ return}
// ConfigurationReport(parameterNumber: 9, size: 1, configurationValue: [2], scaledConfigurationValue: 2)  Fast Recovery
    if(!recovery){recovery = 2}
 
    if (recovery == "fast"){value = 1}
    if (recovery == "economy"){value = 2}
   
    logging("Set Recovery to:${recovery} ", "info")
    
   logging("Sending >>  configurationSet (parameterNumber: 9, size: 1, configurationValue: [${value}])  Get(config 9)", "trace")  
   
   delayBetween([    
   zwave.configurationV2.configurationSet(parameterNumber: 9, size: 1, configurationValue: [value]).format(),
   zwave.configurationV2.configurationGet(parameterNumber: 9).format(),    
	], 2500)  

}


void getIcons(){
    state.icon ="<img src='https://github.com/tmastersmart/hubitat-code/raw/main/images/radio-thermostat.jpg'>"
    if(state.model =="CT30" || state.model =="CT30e rev.01" || state.model =="CT30e"){state.icon ="<img src='https://github.com/tmastersmart/hubitat-code/raw/main/images/ct30.jpg'> <a href='https://www.opensmarthouse.org/zwavedatabase/94'> CT30 Info</a>"}
    if(state.model =="CT30e bad" ){state.icon ="<img src='https://github.com/tmastersmart/hubitat-code/raw/main/images/ct30.jpg'> CT30e bad<a href='https://www.opensmarthouse.org/zwavedatabase/94'> Info</a> Bad Firmware"}
    if(state.model =="CT32"){state.icon ="<img src='https://github.com/tmastersmart/hubitat-code/raw/main/images/ct30.jpg'> <a href='https://www.opensmarthouse.org/zwavedatabase/99'> CT32 Info </a>"}
    if(state.model =="CT50"){state.icon ="<img src='https://github.com/tmastersmart/hubitat-code/raw/main/images/ct50.jpg'> <a href='https://www.manualslib.com/manual/951814/Radio-Thermostat-Ct50.html'> CT50 Manual </a>"  } 
    if(state.model =="CT80"){state.icon ="<img src='https://github.com/tmastersmart/hubitat-code/raw/main/images/ct80.jpg'> <a href='https://www.opensmarthouse.org/zwavedatabase/97'> CT80 Info </a>"}
    if(state.model =="CT100" || state.model =="CT100 Plus"){state.icon ="<img src='https://github.com/tmastersmart/hubitat-code/raw/main/images/ct100.jpg'> <a href='https://www.opensmarthouse.org/zwavedatabase/96'> CT100 Info</a>"}
    if(state.model =="CT101" || state.model =="CT101 iris"){state.icon ="<img src='https://github.com/tmastersmart/hubitat-code/raw/main/images/ct101.jpg'> <a href='https://www.opensmarthouse.org/zwavedatabase/98'> CT101 Info</a>"}
    if(state.model =="CT110"){state.icon ="<img src='https://github.com/tmastersmart/hubitat-code/raw/main/images/radio-thermostat.jpg'><a href='https://www.opensmarthouse.org/zwavedatabase/555'> CT110 Info</a>"}// add image
    if(state.model =="CT200" || state.model =="CT200 Vivant" || state.model == "CT200x Vivant"){state.icon ="<img src='https://github.com/tmastersmart/hubitat-code/raw/main/images/ct200.jpg'> <a href='https://www.opensmarthouse.org/zwavedatabase/98'> CT200 Info</a>"}
    if(state.model =="CT200x Vivant"){state.icon ="<img src='https://github.com/tmastersmart/hubitat-code/raw/main/images/ct200.jpg'> <a href='https://www.opensmarthouse.org/zwavedatabase/938'> CT200x Info</a>"}


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

/*

Radio Thermostat CT30 hubitat driver
Radio Thermostat CT32 hubitat driver
Radio Thermostat CT50 hubitat driver
Radio Thermostat CT80 hubitat driver
Radio Thermostat CT100 hubitat driver
Radio Thermostat CT101 hubitat driver
Radio Thermostat CT110 hubitat driver
Radio Thermostat CT200 hubitat driver

*/
