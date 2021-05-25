/*Observer IP link

Ambent weather station driver for MMPWS scripts

http://pws.winnfreenet.com get script
https://github.com/tmastersmart/hubitat-code/blob/main/observer_ip_link.groovy



Script reads from station and post to hub localy
with no need to pull data from the Ambent Weather

This is the driver that the script post to.
Also need Maker API installed and setup.

The script needs this device ID the device ID of the maker API and the Token

v1 beta   05/25/2021

*/

metadata {
    definition (name: "PWS Local Weather Station", namespace: "tmastersmart", author: "tmaster",importUrl: "https://github.com/tmastersmart/hubitat-code/raw/main/observer_ip_link.groovy") {

        capability "Illuminance Measurement"
        capability "Relative Humidity Measurement"
        capability "Temperature Measurement"
        capability "Pressure Measurement"
        capability "Water Sensor"    
        capability "Ultraviolet Index"
        capability "Battery"
        capability "Motion Sensor"
        capability "Power Meter"
        capability "Energy Meter"
        
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

        attribute "windDirection", "number"     //Hubitat  OpenWeather
        attribute "windSpeed", "number"         //Hubitat  OpenWeather
        attribute "wind_cardinal", "string"
		attribute "Rain", "Number"
        attribute "Wind", "Number"
        attribute "Gust", "Number"
        attribute "PWSDate", "Number"
        attribute "Dew", "Number"
        attribute "Battery", "Number"
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
   /* arrived()
    accelerationInactive()
    COClear()
    close()
    setIlluminance(50)
    setCarbonDioxide(350)
    setRelativeHumidity(35)
    motionInactive()
    smokeClear()
    setTemperature(70)
    dry()*/
    runIn(1800,logsOff)
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


def setIlluminance(lux) {
    def descriptionText = "${device.displayName} Illuminance is ${lux} lux"
    if (txtEnable) log.info "${descriptionText}"
    sendEvent(name: "illuminance", value: lux, descriptionText: descriptionText, unit: "Lux")
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
    sendEvent(name: "PWSDate", value: PWSDate, descriptionText: descriptionText, unit: "Date")
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
