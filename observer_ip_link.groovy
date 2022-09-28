/*PWS Observer IP Link

Ambent weather station driver for MMPWS scripts
Get script here v3.2.7 and above http://pws.winnfreenet.com 

Copyright pws.winnfreenet.com 2021/2022

Compatible with: 
WS-0800-IP WS-0900-IP WS-1200 WS-1200-IP WS-1201 WS-1201-IP WS-1400-IP 
WS-1401-IP WS-1550-IP WS-1000-WiFi WS-1001-WiFi WS-1002-WiFi


https://github.com/tmastersmart/hubitat-code/blob/main/observer_ip_link.groovy

SolarRad must be set to W/m2 on station. 


Script reads from station and post to hub localy
with no need to pull data from the Ambent Weather

This is the driver that the script will post to.
Also need Maker API installed and setup.

Create a Outdoor and indoor sensor using this driver
Give permission in the API maker then enter the 
ID #s for the sensors and API in the script.

v1.8        09/28/2022 Updated logging
v1.7        09/02/2022  attribute "WU" added
v1.6        08/18/2022  Fix numeric varables so scripts can use math 
v1.5        06-10-2021
v1.4    
v1.3        06/07/2021
v1.2 beta   06/02/2021

    

*/
def clientVersion() {
    TheVersion="1.8"
 if (state.version != TheVersion){ 
     state.version = TheVersion
     configure() // Forces config on updates
 }
}


metadata {
    definition (name: "PWS Observer IP Link", namespace: "tmastersmart", author: "tmaster",importUrl: "https://github.com/tmastersmart/hubitat-code/raw/main/observer_ip_link.groovy") {

        capability "Illuminance Measurement"
        capability "Relative Humidity Measurement"
        capability "Temperature Measurement"
        capability "Pressure Measurement"
        capability "Water Sensor"    
        capability "Ultraviolet Index"
        capability "Battery"
        capability "Motion Sensor"
        capability "Power Meter"
        capability "PowerMeter"
        capability "EnergyMeter"
        capability "Energy Meter"
        
        command "setUVI", ["Number"]
        command "setRAD", ["Number"]
        command "setName", ["Number"]        
        command "setCWOP",["string"]
        command "setPWS",["string"]
        command "setWeather",["string"]
        command "setModel", ["string"]
        command "setVersion", ["string"]
        command "setAgent", ["string"]
        command "setBattery", ["Number"]
        command "setIlluminance", ["Number"]
        command "setRelativeHumidity", ["Number"]
        command "setTemperature", ["Number"]
        command "setPressure", ["Number"]
        command "setAltemeter", ["Number"]
        command "setRainR", ["Number"]
        command "setRainD", ["Number"]
        command "setRainH", ["Number"]
        command "setRainM", ["Number"]
        command "setRainY", ["Number"]
        command "setRain24", ["Number"]
        command "setWind", ["Number"]
        command "setWindDirection", ["Number"]
        command "setWind_cardinal", ["Number"]
        command "setGust", ["Number"]
        command "setDGust", ["Number"]
        command "setPWSDate", ["Number"]
        command "setDew", ["Number"]  
        command "wet"
        command "dry"
        
        
        command "initialize" 
        

        
        attribute "weather", "string"                   
        attribute "Agent", "string"
        attribute "Model", "string"
        attribute "name", "string"
        attribute "CWOP", "string"
        attribute "PWS", "string"
        attribute "WU", "string"
        
        attribute "ultraviolet", "string"
		attribute "Rain", "Number"       
        attribute "RainDaily", "Number"
        attribute "RainRate", "Number"
        attribute "RainHour", "Number"
        attribute "RainMonth", "Number"
        attribute "RainYear", "Number"
        attribute "Rain24", "Number"
        attribute "Wind", "Number"   
        attribute "WindDirection", "Number"    //Hubitat  OpenWeather
        attribute "WindSpeed", "Number"        //Hubitat  OpenWeather
        attribute "WindDGust", "Number"
        attribute "WindGust", "Number"
        attribute "Altemeter", "Number"
        attribute "Pressure", "Number"
        attribute "Temperature", "Number"
        attribute "RelativeHumidity", "Number"
        attribute "humidity", "Number"
        attribute "Gust", "Number"
        attribute "PWSDate", "Number"
        attribute "Dew", "Number"
        attribute "Battery", "Number"
        attribute "uvi", "Number"
        attribute "solarrad", "Number"
        attribute "untraviolet", "Number"
        attribute "Wind_cardinal", "Number"

      
    }
    preferences {
         input name: "infoLogging",  type: "bool", title: "Enable info logging", description: "Recomended low level" ,defaultValue: true,required: true
	     input name: "debugLogging", type: "bool", title: "Enable debug logging", description: "MED level Debug" ,defaultValue: false,required: true
	     input name: "traceLogging", type: "bool", title: "Enable trace logging", description: "Insane HIGH level", defaultValue: false,required: true

    }
}



def installed() {
    log.warn "installed..."
    runIn(1800,logsOff)
    updateDataValue("manufacturer", "Ambent Weather")
    updateDataValue("model", "Observer IP")
    updateDataValue("firmware", "Observer IP")
}

def initialize() {
    updateDataValue("manufacturer", "Ambent Weather")
    updateDataValue("model", "Observer IP")
	updateDataValue("firmware", "Observer IP")
    state.remove("gust")
}
    

def updated() {
    logging("${device} : updated..", "info")
    clientVersion()
    loggingUpdate()
}
def  configure(){
    
}

def parse(String description) {

}

def setBattery(battery) {
    logging("${device} : Battery is ${battery} %", "debug")
    sendEvent(name: "battery", value: battery, unit: "%")
}
def setVersion(version) { updateDataValue("firmware", version)}
def setModel(version)   { updateDataValue("model", version)}
def setAgent(agent)     { updateDataValue("agent", agent)}


def setWeather(weather) {
    logging("${device} : Weather status ${weather}", "debug")
    sendEvent(name: "weather", value: weather, unit: "text")
}
def setIlluminance(lux) {
    logging("${device} : Illuminance is ${lux} lux", "debug")
    sendEvent(name: "illuminance", value: lux,  unit: "Lux")
}

def setUVI(uvi) {
    logging("${device} : UVI Index is ${uvi} ", "debug")
    sendEvent(name: "ultraviolet", value: uvi, unit: "uvi")
    sendEvent(name: "uvi", value: uvi,  unit: "uvi")
}
def setRAD(solarrad) {
    logging("${device} : Solar Rad is ${solarrad} wm2", "debug")
    sendEvent(name: "solarrad", value: solarrad, unit: "wm2")
	sendEvent(name: "energy", value: solarrad, unit: "wm2")
    sendEvent(name: "power", value: solarrad, unit: "wm2")
}


def setRelativeHumidity(humid) {
    logging("${device} : RelativeHumidity is ${humid}% humidity", "debug")
    sendEvent(name: "humidity", value: humid, unit: "RH%")
}

def setTemperature(temp) {
    def unit = "°${location.temperatureScale}"
    logging("${device} : Temperature is ${temp}${unit}", "info")
    sendEvent(name: "temperature", value: temp, descriptionText: "Temperature is ${temp}${unit}", unit: unit)
}

def setPressure(Pressure) {
    logging("${device} : Relative Pressure in inga is ${Pressure}", "debug")
    sendEvent(name: "pressure", value: Pressure,  unit: "inga")
}

def setName(name) {
    logging("${device} : Station Name is ${name}", "debug")
    updateDataValue("WU", name)
    sendEvent(name: "name", value: name, unit: "text")
}
def setCWOP(name) {
    logging("${device} : CWOP Name is ${name}", "debug")
    updateDataValue("CWOP", name)
    sendEvent(name: "CWOP", value: name,  unit: "text")
}
def setPWS(name) {
    logging("${device} : PWS Name is ${name}", "debug")
    updateDataValue("PWS", name)
    sendEvent(name: "PWS", value: name, unit: "text")
}
def setAltemeter(Altemeter) {
    logging("${device} : Altemeter in Mbar is ${Altemeter}", "debug")
    sendEvent(name: "Altemeter", value: Altemeter,  unit: "mbar")
}
def setRainR(Rain) {
    logging("${device} : Rain Rate is ${Rain}", "debug")
    sendEvent(name: "RainRate", value: Rain,  unit: "Inches")
}
def setRainD(Rain) {
    logging("${device} : Daily Rain is ${Rain}", "debug")
    sendEvent(name: "RainDaily", value: Rain, unit: "Inches")
    sendEvent(name: "Rain", value: Rain,  unit: "Inches")

}
def setRainH(Rain) {
    logging("${device} : Hourly Rain is ${Rain}", "debug")
    sendEvent(name: "RainHour", value: Rain, descriptionText: descriptionText, unit: "Inches")
}
def setRainM(Rain) {
    logging("${device} : Monthly is ${Rain}", "debug")
    sendEvent(name: "RainMonth", value: Rain,  unit: "Inches")
}
def setRainY(Rain) {
    logging("${device} : Yearly Rain is ${Rain}", "debug")
    sendEvent(name: "RainYear", value: Rain,  unit: "Inches")
}

def setRain24(Rain) {
    logging("${device} : Last 24 Hrs Rain is ${Rain}", "debug")
    sendEvent(name: "Rain24", value: Rain, unit: "Inches")
}

def setWind(Wind) {
    logging("${device} : Current Wind is ${Wind}", "debug")
    sendEvent(name: "Wind", value: Wind, descriptionText: descriptionText, unit: "mph")
    sendEvent(name: "WindSpeed", value: Wind, descriptionText: descriptionText, unit: "mph")
}
def setGust(Gust) {
    logging("${device} : Wind Gusting at ${Gust}", "debug")
    sendEvent(name: "WindGust", value: Gust,  unit: "mph")
    sendEvent(name: "Gust", value: Gust,  unit: "mph")
}

def setDGust(Gust) {
    logging("${device} : Daily Wind Gust is ${Gust}", "debug")
    sendEvent(name: "WindDGust", value: Gust, unit: "mph")
}

def setWindDirection(windDirection) {
    logging("${device} : Wind Direction is ${windDirection}", "debug")
    sendEvent(name: "WindDirection", value: windDirection, unit: "dir")
}
def setWind_cardinal(wind_cardinal) {
    logging("${device} : Wind cardinal is ${wind_cardinal}", "debug")
    sendEvent(name: "Wind_cardinal", value: wind_cardinal,  unit: "dir")
}
// Date is last value posted. 
def setPWSDate(PWSDate) {
    logging("${device} : Date is ${PWSDate}", "debug")
    sendEvent(name: "PWSDate", value: PWSDate,  unit: "")
}
def setDew(Dew) {
    logging("${device} : Dewpoint is ${Dew}", "debug")
    sendEvent(name: "Dew", value: Dew,  unit: "deg")
}

def wet() {
    logging("${device} : wet", "debug")
    sendEvent(name: "water", value: "wet")
}

def dry() {
    logging("${device} : dry", "debug")
    sendEvent(name: "water", value: "dry")
}

// Logging block 
//	device.updateSetting("infoLogging",[value:"true",type:"bool"])
void loggingUpdate() {
    logging("${device} : Logging Info:[${infoLogging}] Debug:[${debugLogging}] Trace:[${traceLogging}]", "infoBypass")
    // Only do this when its needed
    if (debugLogging){runIn(3600,debugLogOff)}
    if (traceLogging){runIn(1800,traceLogOff)}
}
void loggingStatus() {logging("${device} : Logging Info:[${infoLogging}] Debug:[${debugLogging}] Trace:[${traceLogging}]", "infoBypass")}
void traceLogOff(){
	device.updateSetting("traceLogging",[value:"false",type:"bool"])
	log.trace "${device} : Trace Logging : Automatically Disabled"
}
void debugLogOff(){
	device.updateSetting("debugLogging",[value:"false",type:"bool"])
	log.debug "${device} : Debug Logging : Automatically Disabled"
}
private logging(String message, String level) {
    if (level == "infoBypass"){log.info  "$message"}
	if (level == "error"){     log.error "$message"}
	if (level == "warn") {     log.warn  "$message"}
	if (level == "trace" && traceLogging) {log.trace "$message"}
	if (level == "debug" && debugLogging) {log.debug "$message"}
    if (level == "info"  && infoLogging)  {log.info  "$message"}
}

