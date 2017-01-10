package linguistic.api

import akka.http.scaladsl.server.Directives

trait BaseApi extends Directives {
  val apiPrefix = shared.Routes.pref / shared.Routes.v1
}
