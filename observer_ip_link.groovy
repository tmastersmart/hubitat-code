/*Observer IP link

Ambent weather station driver for MMPWS scripts

Compatible with: WS-0800-IP WS-0900-IP WS-1200 WS-1200-IP WS-1201 WS-1201-IP WS-1400-IP WS-1401-IP WS-1550-IP WS-1000-WiFi WS-1001-WiFi WS-1002-WiFi

http://pws.winnfreenet.com get script here
https://github.com/tmastersmart/hubitat-code/blob/main/observer_ip_link.groovy

SolarRad must be set to W/m2 on station. 


Script reads from station and post to hub localy
with no need to pull data from the Ambent Weather

This is the driver that the script will post to.
Also need Maker API installed and setup.

Create a Outdoor and indoor sensor using this driver
Give permission in the API maker then enter the 
ID #s for the sensors and API in the script.


v1.2 beta   06/02/2021
v1.3        06/07/2021
*/

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
        
        command "setModel", ["Number"]
        command "setVersion", ["Number"]
        command "setAgent", ["Number"]
        command "setBattery", ["Number"]
        command "setIlluminance", ["Number"]
        command "setRelativeHumidity", ["Number"]
        command "setTemperature", ["Number"]
        command "setPressure", ["Number"]
        command "setRain", ["Number"]
        command "setWind", ["Number"]
        command "setWindDirection", ["Number"]
        command "setWind_cardinal", ["Number"]
        command "setGust", ["Number"]
        command "setPWSDate", ["Number"]
        command "setDew", ["Number"]  
        command "wet"
        command "dry" 
        command "initialize" 
        command "setUVI", ["Number"]
        command "setRAD", ["Number"]
                           
        attribute "windDirection", "number"     //Hubitat  OpenWeather
        attribute "windSpeed", "number"         //Hubitat  OpenWeather
        attribute "wind_cardinal", "string"
		attribute "Rain", "Number"
        attribute "Wind", "Number"
        attribute "Gust", "Number"
        attribute "PWSDate", "Number"
        attribute "Dew", "Number"
        attribute "Battery", "Number"
        attribute "uvi", "Number"
    }
    preferences {
        input name: "logEnable", type: "bool", title: "Enable debug logging", defaultValue: true
        input name: "txtEnable", type: "bool", title: "Enable descriptionText logging", defaultValue: true
    }
}

def logsOff(){
    log.warn "debug logging disabled..."
    device.updateSetting("logEnable",[value:"false",type:"bool"])
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
}
    

def updated() {
    log.info "updated..."
    log.warn "debug logging is: ${logEnable == true}"
    log.warn "description logging is: ${txtEnable == true}"
    if (logEnable) runIn(1800,logsOff)
}

def parse(String description) {

}

def setBattery(battery) {
    def descriptionText = "${device.displayName} Battery is ${battery} %"
    if (txtEnable) log.info "${descriptionText}"
    sendEvent(name: "battery", value: battery, descriptionText: descriptionText, unit: "%")
}
def setVersion(version) {
    def descriptionText = "${device.displayName} Firmware is ${version} "
//    if (txtEnable) log.info "${descriptionText}"
    updateDataValue("firmware", version)
//    sendEvent(name: "firmware", value: version, descriptionText: descriptionText, unit: "")
}

def setModel(version) {
    def descriptionText = "${device.displayName} Model is ${version} "
//    if (txtEnable) log.info "${descriptionText}"
    updateDataValue("model", version)
//    sendEvent(name: "firmware", value: version, descriptionText: descriptionText, unit: "")
}

def setAgent(agent) {
    def descriptionText = "${device.displayName} Software is ${agent} "
//    if (txtEnable) log.info "${descriptionText}"
    updateDataValue("agent", agent)
//  This is the version of MMPWS running on another PC
}


def setIlluminance(lux) {
    def descriptionText = "${device.displayName} Illuminance is ${lux} lux"
    if (txtEnable) log.info "${descriptionText}"
    sendEvent(name: "illuminance", value: lux, descriptionText: descriptionText, unit: "Lux")
}

def setUVI(uvi) {
    def descriptionText = "${device.displayName} UVI Index is ${uvi} "
    if (txtEnable) log.info "${descriptionText}"
    sendEvent(name: "ultraviolet", value: uvi, descriptionText: descriptionText, unit: "uvi")
}
def setRAD(solarrad) {
    def descriptionText = "${device.displayName} Solar Rad is ${solarrad} "
    if (txtEnable) log.info "${descriptionText}"
    sendEvent(name: "solarrad", value: solarrad, descriptionText: descriptionText, unit: "wm2")
	sendEvent(name: "energy", value: solarrad, unit: "wm2")
    sendEvent(name: "power", value: solarrad, unit: "wm2")
}


def setRelativeHumidity(humid) {
    def descriptionText = "${device.displayName} RelativeHumidity is ${humid}% humidity"
    if (txtEnable) log.info "${descriptionText}"
    sendEvent(name: "humidity", value: humid, descriptionText: descriptionText, unit: "RH%")
}

def setTemperature(temp) {
    def unit = "°${location.temperatureScale}"
    def descriptionText = "${device.displayName} Temperature is ${temp}${unit}"
    if (txtEnable) log.info "${descriptionText}"
    sendEvent(name: "temperature", value: temp, descriptionText: descriptionText, unit: unit)
}

def setPressure(Pressure) {
    def descriptionText = "${device.displayName}  Pressure is ${Pressure}"
    if (txtEnable) log.info "${descriptionText}"
    sendEvent(name: "pressure", value: Pressure, descriptionText: descriptionText, unit: "mbar")
}

def setRain(Rain) {
    def descriptionText = "${device.displayName}  Rain is ${Rain}"
    if (txtEnable) log.info "${descriptionText}"
    sendEvent(name: "Rain", value: Rain, descriptionText: descriptionText, unit: "Inches")
}

def setWind(Wind) {
    def descriptionText = "${device.displayName}  Wind is ${Wind}"
    if (txtEnable) log.info "${descriptionText}"
    sendEvent(name: "Wind", value: Wind, descriptionText: descriptionText, unit: "mph")
    sendEvent(name: "windSpeed", value: Wind, descriptionText: descriptionText, unit: "mph")
}

def setWindDirection(windDirection) {
    def descriptionText = "${device.displayName}  Wind Direction is ${windDirection}"
    if (txtEnable) log.info "${descriptionText}"
    sendEvent(name: "windDirection", value: windDirection, descriptionText: descriptionText, unit: "mph")
}
def setWind_cardinal(wind_cardinal) {
    def descriptionText = "${device.displayName}  Wind cardinal is ${wind_cardinal}"
    if (txtEnable) log.info "${descriptionText}"
    sendEvent(name: "wind_cardinal", value: wind_cardinal, descriptionText: descriptionText, unit: "dir")
}



def setGust(Gust) {
    def descriptionText = "${device.displayName}  Wind Gust is ${Gust}"
    if (txtEnable) log.info "${descriptionText}"
    sendEvent(name: "Gust", value: Gust, descriptionText: descriptionText, unit: "mph")
}
def setPWSDate(PWSDate) {
    def descriptionText = "${device.displayName}  Date is ${PWSDate}"
    if (txtEnable) log.info "${descriptionText}"
    sendEvent(name: "PWSDate", value: PWSDate, descriptionText: descriptionText, unit: "")
}
def setDew(Dew) {
    def descriptionText = "${device.displayName}  Dewpoint is ${Dew}"
    if (txtEnable) log.info "${descriptionText}"
    sendEvent(name: "Dew", value: Dew, descriptionText: descriptionText, unit: "deg")
}

def wet() {
    def descriptionText = "${device.displayName} water wet"
    if (txtEnable) log.info "${descriptionText}"
    sendEvent(name: "water", value: "wet", descriptionText: descriptionText)
}

def dry() {
    def descriptionText = "${device.displayName} water dry"
    if (txtEnable) log.info "${descriptionText}"
    sendEvent(name: "water", value: "dry", descriptionText: descriptionText)
}
