package app.palavrascruzadas.data

import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId

/**
 * App clock. With "simulated date" (test builds only), the day is the stored one;
 * otherwise it is the phone's clock. Injectable: every date calculation goes through here.
 */
object AppClock {
    fun forSimulatedDay(simulatedDay: Long?): Clock =
        if (simulatedDay == null) {
            Clock.systemDefaultZone()
        } else {
            val zone = ZoneId.systemDefault()
            Clock.fixed(LocalDate.ofEpochDay(simulatedDay).atStartOfDay(zone).toInstant(), zone)
        }

    fun today(simulatedDay: Long?): LocalDate = LocalDate.now(forSimulatedDay(simulatedDay))
}
