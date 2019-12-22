package com.supersonic

import ai.x.play.json.Jsonx
import play.api.libs.json.Format

case class Module(module: String,
                  dependencies: List[String],
                  libraryDependencies: List[String],
                  sourcePath: String)
object Module {
  implicit val fmt: Format[Module] = Jsonx.formatCaseClass[Module]
}
