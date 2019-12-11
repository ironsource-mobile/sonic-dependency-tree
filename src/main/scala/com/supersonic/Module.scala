package com.supersonic

import ai.x.play.json.Jsonx

case class Module(module: String,
                  dependencies: List[String],
                  libraryDependencies: List[String],
                  sourcePath: String)
object Module {
  implicit val fmt = Jsonx.formatCaseClass[Module]
}
