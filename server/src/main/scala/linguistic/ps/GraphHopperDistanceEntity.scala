package linguistic.ps

import java.nio.file.Paths
import java.util.Locale

import com.graphhopper.{GHRequest, GraphHopper}
import com.graphhopper.config.{CHProfile, Profile}

object GraphHopperDistanceEntity {

  type Distance = Double
  type TimeInMs = Long

  val DATA_DIR      = "maps"
  val PBF_FILE_NAME = "ontario-latest.osm.pbf"

  final case class Coordinates(name: String, lat: Double, lon: Double) {
    override def toString(): String = name
  }

  /**
    * https://github.com/softwaremill/vehicle-routing-problem-java.git
    */
  def create(): GraphHopper = {
    val hopper = new GraphHopper()
    hopper.setOSMFile(Paths.get(DATA_DIR, PBF_FILE_NAME).toString())
    hopper.setGraphHopperLocation(".cache/routing-graph")
    hopper.setProfiles(new Profile("car").setVehicle("car").setWeighting("fastest").setTurnCosts(false))
    //hopper.setProfiles(new Profile("car").setName("car").setWeighting("fastest"))
    hopper.getCHPreparationHandler().setCHProfiles(new CHProfile("car"))
    hopper.importOrLoad()
    hopper
  }

  def find(from: Coordinates, to: Coordinates)(implicit gh: GraphHopper): Option[Distance] = {
    val req      = new GHRequest(from.lat, from.lon, to.lat, to.lon).setProfile("car").setLocale(Locale.ENGLISH)
    val response = gh.route(req)
    if (response.hasErrors()) {
      println(response.getErrors.toString)
      None
    } else {
      val path             = response.getBest
      val distanceInMeters = path.getDistance()
      // val timeInMs = path.getTime()
      // Duration.ofMillis(timeInMs)
      Some(distanceInMeters)
    }
  }
}
