/*
    Copyright (c) 2015-2020 Sam Gleske - https://github.com/samrocketman/jenkins-script-console-scripts

    Permission is hereby granted, free of charge, to any person obtaining a copy of
    this software and associated documentation files (the "Software"), to deal in
    the Software without restriction, including without limitation the rights to
    use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
    the Software, and to permit persons to whom the Software is furnished to do so,
    subject to the following conditions:

    The above copyright notice and this permission notice shall be included in all
    copies or substantial portions of the Software.

    THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
    IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
    FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
    COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
    IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
    CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
*/
/*
   Find all cron schedules in a Jenkins instance organized by frequency
   (weekly, daily, hourly, other).  This script is compatible with all Jenkins
   job types and recurses into Jenkins folders.

   This script was inspired by:
     https://gist.github.com/mgedmin/d6d271f4ee6ae82d9a86
 */

import hudson.model.Descriptor
import hudson.model.Job
import hudson.triggers.TimerTrigger
import java.util.regex.Pattern
import jenkins.model.Jenkins

/*
   FUNCTIONS
 */

boolean patternMatchesAnyLine(String pattern, String text) {
    text.tokenize('\n').any { String line ->
        Pattern.compile(pattern).matcher(line.trim()).matches()
    }
}

boolean isWeeklySchedule(String cron) {
    patternMatchesAnyLine('^([^ ]+ +){2}\\* +\\* +[^*]+$', cron)
}

boolean isDailySchedule(String cron) {
    patternMatchesAnyLine('^([^ *]+ +){2}\\* +\\* +\\*+$', cron)
}

boolean isHourlySchedule(String cron) {
    patternMatchesAnyLine('^[^ ]+ +\\* +\\* +\\*+$', cron)
}

void displaySchedule(String frequency, Map schedules) {
    int half = ((80 - frequency.size()) / 2) - 1
    int padding = 80 - (2*half + frequency.size()) - 2
    println(['='*half, frequency, '='*half].join(' ') + '='*padding)
    schedules.each { k, v ->
        println(k)
        println('    ' + v.tokenize('\n').join('\n    '))
    }
    println('='*80)
}

/*
   MAIN LOGIC
 */


// Find all schedules.
Jenkins j = Jenkins.instance
Descriptor cron = j.getDescriptor(TimerTrigger)
Map<String, String> schedules = j.getAllItems(Job).findAll { Job job ->
  job?.triggers?.get(cron)
}.collect { Job job ->
  	[ (job.fullName):  job.triggers.get(cron).spec ]
}.sum()


// Gather schedules organized by their frequency.
Map weekly = schedules.findAll { k, v ->
    isWeeklySchedule(v)
} ?: [:]
Map daily = schedules.findAll { k, v ->
    isDailySchedule(v)
} ?: [:]
Map hourly = schedules.findAll { k, v ->
    isHourlySchedule(v)
} ?: [:]
Map other = schedules.findAll { k, v ->
    !isWeeklySchedule(v) &&
    !isDailySchedule(v) &&
    !isHourlySchedule(v)
} ?: [:]


// Print out schedules after they're organized.
displaySchedule('Weekly Schedules', weekly)
displaySchedule('Daily Schedules', daily)
displaySchedule('Hourly Schedules', hourly)
displaySchedule('Other Schedules', other)
