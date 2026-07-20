/**
 *  Pool / Spa / Drainage Pump Scheduler v2.1
 * 
 *
 *
 *
 *
 *
 *
 */

definition (name: "Pump Scheduler", 
    namespace: "tmastersmart", 
    author: "Tmaster", 
    importUrl: "https://raw.githubusercontent.com/tmastersmart/hubitat-code/main/pump_scheduler.groovy",
    description: "Smart scheduler for Pool, Spa, Drainage Pumps with daily runtime and cooldown",
    category: "Convenience",
    iconUrl: "",
    iconX2Url: "",
    iconX3Url: ""
)

preferences {
    section {
        paragraph """
            <h2 style='text-align:center'>Pump Scheduler</h2>
            <p style='text-align:center'>Schedule your Pool, Spa, Drainage Pump, or any equipment that needs timed runs with cooldown periods.</p>
            <p style='text-align:center'>Great for preventing overheating and extending equipment life.</p>
        """
    }
    
    section("Pump Device") {
        input "pump", "capability.switch", title: "Select Your Pump", required: true
        input "deviceType", "enum", title: "Device Type", options: ["Pool Pump", "Spa Pump", "Drainage Pump", "Other"], required: true, defaultValue: "Pool Pump"
    }
    
    section("Daily Schedule") {
        input "dailyHours", "decimal", title: "Total Hours to Run Per Day", required: true, defaultValue: 8.0, range: "1..24"
        input "maxRunPerCycle", "decimal", title: "Maximum Continuous Run Time (hours)", required: true, defaultValue: 4.0, range: "0.5..12"
        
        input "startOption", "enum", title: "Start Time", options: ["Specific Time", "Sunrise", "Sunset"], required: true, defaultValue: "Specific Time"
        input "startTime", "time", title: "Specific Start Time", required: false
    }
    
    section("Cooldown Settings") {
        input "cooldownMinutes", "number", title: "Cooldown Time After Each Run (minutes)", required: true, defaultValue: 30, range: "15..180"
    }
    
    section("Advanced Options") {
        input "maxCyclesPerDay", "number", title: "Maximum Cycles Per Day (0 = unlimited)", required: true, defaultValue: 0
        input "name", "text", title: "Custom App Name (optional)", required: false
    }
}

def clientVersion() {
    TheVersion="1.0.3"
 if (state.version != TheVersion){ 
     state.version = TheVersion
 }
}

def installed() {
    log.info "${deviceType} Scheduler Installed"
    initialize()
}

def updated() {
    log.info "${deviceType} Scheduler Updated"
    unsubscribe()
    unschedule()
    initialize()
}

def initialize() {
    state.totalRuntimeToday = 0.0
    state.cyclesToday = 0
    state.isRunning = false
    
    scheduleStartTime()
    
    runEvery1Minute(monitorPump)
    
    log.info "Scheduler initialized - ${dailyHours} hrs/day | Max ${maxRunPerCycle} hrs per cycle"
}

def scheduleStartTime() {
    unschedule("startDailySchedule")
    
    if (startOption == "Sunrise") {
        schedule("0 0 * * * ?", startDailySchedule)  // This is placeholder - we'll use sunrise below
        subscribe(location, "sunriseTime", "startDailySchedule")
        log.info "Scheduled to start at Sunrise"
    } 
    else if (startOption == "Sunset") {
        subscribe(location, "sunsetTime", "startDailySchedule")
        log.info "Scheduled to start at Sunset"
    } 
    else if (startTime) {
        schedule(startTime, startDailySchedule)
        log.info "Scheduled to start at ${startTime}"
    } 
    else {
        startDailySchedule()
    }
}

def startDailySchedule(evt = null) {
    state.totalRuntimeToday = 0.0
    state.cyclesToday = 0
    log.info "New daily cycle started"
    startPumpCycle()
}

def startPumpCycle() {
    if (state.isRunning) return
    
    if (maxCyclesPerDay > 0 && state.cyclesToday >= maxCyclesPerDay) {
        log.info "Max cycles per day reached"
        return
    }
    
    def remaining = (dailyHours * 3600) - state.totalRuntimeToday
    if (remaining <= 0) return
    
    def runSeconds = Math.min(remaining, maxRunPerCycle * 3600)
    
    pump.on()
    state.isRunning = true
    state.currentCycleStart = now()
    state.cyclesToday++
    
    runIn(runSeconds.toInteger(), stopPumpCycle)
    
    log.info "${deviceType} turned ON"
}

def stopPumpCycle() {
    if (!state.isRunning) return
    
    pump.off()
    def actualRun = (now() - state.currentCycleStart) / 1000
    state.totalRuntimeToday += actualRun
    state.isRunning = false
    
    log.info "${deviceType} turned OFF after ${actualRun/3600} hours"
    
    if (state.totalRuntimeToday < dailyHours * 3600) {
        runIn(cooldownMinutes * 60, startPumpCycle)
    }
}

def monitorPump() {
    if (state.isRunning && pump.currentSwitch == "off") {
        log.warn "Pump was turned off externally - restarting"
        startPumpCycle()
    }
}
