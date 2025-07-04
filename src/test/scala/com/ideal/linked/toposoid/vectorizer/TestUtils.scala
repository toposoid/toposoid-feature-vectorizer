
package com.ideal.linked.toposoid.vectorizer

import io.jvm.uuid.UUID

object TestUtils {

  var usedUuidList = List.empty[String]

  def getUUID(): String = {
    var uuid: String = UUID.random.toString
    while (usedUuidList.filter(_.equals(uuid)).size > 0) {
      uuid = UUID.random.toString
    }
    usedUuidList = usedUuidList :+ uuid
    uuid
  }
}
